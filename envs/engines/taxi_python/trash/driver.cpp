#include <ctime>
#include <tuple>
#include <pybind11/pybind11.h>
#include <pybind11/numpy.h>
#include <cstdio>

// #include "order.cpp"


namespace py = pybind11;
using namespace std;

py::module_ pickle = py::module_::import("pickle");
py::object Order = py::module_::import("Core.simulator.entity").attr("Order");
py::object Grids = py::module_::import("Core.simulator.grids").attr("Grids");
py::object join = py::module_::import("os.path").attr("join");
py::object open = py::module_::import("io").attr("open");
string DATA_PATH = py::module_::import("Core.settings").attr("DATA_PATH").cast<string>();
bool IS_REPO_CAN_SERVE = py::module_::import("Core.settings").attr("IS_REPO_CAN_SERVE").cast<bool>();
bool IS_TRACK = py::module_::import("Core.settings").attr("IS_TRACK").cast<bool>();
int SPEED = py::module_::import("Core.settings").attr("SPEED").cast<int>();
int IDLE_TRANSITION_FREQUENCY = py::module_::import("Core.settings").attr("IDLE_TRANSITION_FREQUENCY").cast<int>();
int REPO_TRANSITION_FREQUENCY = py::module_::import("Core.settings").attr("REPO_TRANSITION_FREQUENCY").cast<int>();
py::function acc_dist = py::module_::import("Core.simulator.utils").attr("acc_dist");
py::function transition = py::module_::import("Core.simulator.utils").attr("transition");

enum State {OFF, IDLE, REPO, SERVE};
// srand(time(0));

struct Driver {
    int log_on_time;
    int t1, t2;
    float lng, lat;
    string grid, hashcode;
    State state;
    bool can_be_repo;
    int timer;
    py::array_t<float> tracks;
    int tracks_p;
    bool is_assign;
    static int global_number;
    static py::array_t<float> log_off_prob;
    Driver(int cur_time=0, float lng=0.0, float lat=0.0, string grid="", bool can_be_repo=true):
    log_on_time(cur_time), lng(lng), lat(lat), grid(grid), can_be_repo(can_be_repo), timer(0), tracks_p(0),
    is_assign(true) {
        Driver::global_number++;
        this->hashcode = to_string(global_number);
        this->tracks = py::array_t<float>(0);
    };

    bool log_off(int cur_time_seg) {
        auto log_off_prob_access = log_off_prob.unchecked<1>();
        if (state == IDLE && (rand() % 100 / 101.0) < log_off_prob_access(cur_time_seg)) {
            state = OFF;
            return true;
        }
        return false;
    }

    string get_grid(){
        return this->grid;
    }

    pair<float, float> get_location() {
        return make_pair(this->lng, this->lat);
    }

    bool can_be_repositioned(int repo_freq) {
        return this->can_be_repo && (this->state == IDLE || (this->state == REPO && this->timer % repo_freq == 0));
    }

    void update(int cur_sec) {
        this->timer += 2;
        auto reader = this->tracks.unchecked<2>();
        if (this->timer % IDLE_TRANSITION_FREQUENCY / 2 == 0 && this->state == IDLE) {
            pair<float, float> loc = Grids.attr("gen_random")(this->grid).cast<pair<float, float> >();
            this->lng = loc.first;
            this->lat = loc.second;
        } else if (this->timer % IDLE_TRANSITION_FREQUENCY == 0 && this->state == IDLE) {
            this->grid = transition(cur_sec, this->grid).cast<string>();
            pair<float, float> loc = Grids.attr("gen_random")(this->grid).cast<pair<float, float> >();
            this->lng = loc.first;
            this->lat = loc.second;
        }else if (this->timer % REPO_TRANSITION_FREQUENCY && this->state == REPO) {
            this->tracks_p ++;
            float k = this->timer / this->t2;
            if (k < 1.0) {
                this->lng = (1 - k) * reader(0, 0) + k * reader(1, 0);
                this->lat = (1 - k) * reader(0, 1) + k * reader(1, 1);
                this->grid = Grids.attr("find_grid_by_current")(this->lng, this->lat, this->grid).cast<string>();
                return;
            }
            this->state = IDLE;
            this->timer = 0;
            this->t2 = 0;
        } else if (this->state == SERVE) {
            printf("%s\n", hashcode.c_str());
            while (this->tracks_p < reader.shape(0) && reader(this->tracks_p).t <= this->timer) {
                this->tracks_p ++;
            }
            this->lng = reader(this->tracks_p - 1).lng;
            this->lat = reader(this->tracks_p - 1).lat;
            if (this->tracks_p >= reader.shape(0)) {
                this->state = IDLE;
                this->timer = 0;
                this->grid = Grids.attr("find_grid")(this->lng, this->lat).cast<string>();
                this->is_assign = true;
            }
        }
    }

    void repo_to(string grid_id) {
        this->state = REPO;
        this->timer = 0;
        this->tracks = Driver::gen_track(make_pair(this->lng, this->lat), Grids.attr("get_grid_location")(grid_id).cast<pair<float, float> >(), this->timer, -1);
        this->tracks_p = 0;
        if (!IS_REPO_CAN_SERVE) {
           this->is_assign = false;
        }
    }

    void assign_order(py::object order) {
        this->state = SERVE;
        this->timer = 0;
        this->is_assign = false;
        if (IS_TRACK) {
            this->tracks = order.track;
            this->tracks_p = 0;
        } else {
            this->tracks = Driver::gen_track(make_pair(lng, lat), make_pair(order.attr("finish_lng"), order.attr("finish_lat")), 0, -1);
        }
    }

    bool is_serve() {
        return this->state = SERVE;
    }


private:
    py::array_t<float> gen_track(pair<float, float> from_lng_lat, pair<float, float> to_lng_lat, int time_offset=0, int freq=2) {
        float f_lng = from_lng_lat.first;
        float f_lat = from_lng_lat.second;
        float t_lng = to_lng_lat.first;
        float t_lat = to_lng_lat.second;
        float dist = acc_dist(f_lng, f_lat, t_lng, t_lat).cast<float>();
        int track_numbers = freq > 0?(int)(dist / (freq * SPEED)):2;
        auto res = py::array_t<float>(track_numbers + 1);
        auto writer = res.mutable_unchecked<1>();
        if (freq > 0){
            for (int k = 0; k <= track_numbers; k++) {
                writer(k).t = time_offset + freq * k;
                writer(k).lng =  (1 - k / track_numbers) * f_lng + k / track_numbers * t_lng;
                writer(k).lat = (1 - k / track_numbers) * f_lat + k / track_numbers * t_lat;
            }
        } else {
            for (int k = 0; k <= 1; k++) {
                writer(k).t = (1 - k) * 0 + (k) * ((int)(dist / SPEED) + 1);
                writer(k).lng = (1 - k) * f_lng + k * t_lng;
                writer(k).lat = (1 - k) * f_lat + k * t_lng;
            }
        }
        return res;
    }

};

int Driver::global_number = 0;
py::array_t<float> Driver::log_off_prob = pickle.attr("load")(open(join(DATA_PATH, "driver_log_off"), "rb"));


PYBIND11_MODULE(entity_driver, m){
    // PYBIND11_NUMPY_DTYPE(Template_Track, t, lng, lat);
    py::class_<Driver>(m, "Driver")
        .def(py::init<int, float, float, string, bool>())
        .def("log_off", &Driver::log_off)
        .def("can_be_repositioned", &Driver::can_be_repositioned)
        .def("update", &Driver::update)
        .def("repo_to", &Driver::repo_to)
        .def("assign_order", &Driver::assign_order)
        .def("get_location", &Driver::get_location)
        .def("get_grid", &Driver::get_grid)
        .def("is_serve", &Driver::is_serve)
        .def_readwrite("hashcode", &Driver::hashcode)
        .def_readwrite("lng", &Driver::lng)
        .def_readwrite("lat", &Driver::lat)
        .def_readwrite("is_assign", &Driver::is_assign)
        .def_readwrite("grid", &Driver::grid);
}

/*
bool can_be_repositioned(int repo_freq)
void update()
void repo_to(string grid_id)
void assign_order(Order order)
pair<float, float> get_location()
string get_grid()
*/

// c++ -O3 -Wall -shared -std=c++11 -undefined dynamic_lookup -fPIC $(python3 -m pybind11 --includes) driver.cpp -o entity_driver$(python3-config --extension-suffix)

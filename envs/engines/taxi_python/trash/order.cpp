#include <pybind11/pybind11.h>
#include <pybind11/numpy.h>
#include <string>

namespace py = pybind11;
using namespace std;

struct Template_Track {
    int t;
    float lng, lat;
};

struct Order {
    Order(string &hashcode, float start_lng, float start_lat, float finish_lng, float finish_lat,
    int start_time, int finish_time, string &start_grid, float reward, py::array_t<float> cancel_prob, py::array_t<float> track):
    hashcode(hashcode), start_grid(start_grid), start_lng(start_lng), start_lat(start_lat), finish_lng(finish_lng),
    finish_lat(finish_lat), reward(reward), start_time(start_time), finish_time(finish_time), cancel_prob(cancel_prob),
    track(track){};
    string hashcode, start_grid;
    float start_lng, start_lat, finish_lng, finish_lat, reward;
    int start_time, finish_time;
    py::array_t<float> cancel_prob;
    py::array_t<float > track;
};


PYBIND11_MODULE(entity_order, m){
    // PYBIND11_NUMPY_DTYPE(Template_Track, t, lng, lat);
    py::class_<Order>(m, "Order")
        .def(py::init<string &, float, float, float, float, int, int, string &, float, py::array_t<float>, py::array_t<float > >())
        .def_readwrite("hashcode", &Order::hashcode)
        .def_readwrite("start_grid", &Order::start_grid)
        .def_readwrite("start_lng", &Order::start_lng)
        .def_readwrite("start_lat", &Order::start_lat)
        .def_readwrite("finish_lng", &Order::finish_lng)
        .def_readwrite("finish_lat", &Order::finish_lat)
        .def_readwrite("reward", &Order::reward)
        .def_readwrite("start_time", &Order::start_time)
        .def_readwrite("finish_time", &Order::finish_time)
        .def_readwrite("cancel_prob", &Order::cancel_prob)
        .def_readwrite("track", &Order::track);
}

// compile instruction
// c++ -O3 -Wall -shared -std=c++11 -undefined dynamic_lookup -fPIC $(python3 -m pybind11 --includes) order.cpp -o entity_order$(python3-config --extension-suffix)

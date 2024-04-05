{
    let cur_mat = 100;
    let cur_repo = 100;

    // count
    let count = 0;
    // 累计收入
    let acc_reward = 0;
    // 分段峰值收入
    let highest_seg_reward = 0;
    // 分段平均收入
    let avg_seg_reward = 0;
    // 平均订单数
    let avg_order = 0;
    // 峰值订单数
    let highest_order = 0;
    // 平均应答率
    let avg_ans_rate = 0;
    // 平均拒单率
    let avg_rej_rate = 0;


    // collect all data
    function updateTime(time, data) {
        let matching_time = data['matching_time'] * 1000;
        let repo_time = data['repo_time'] * 1000;
        if (matching_time > cur_mat) {
            $('#matNum').css('color', 'indianred');
            $('#matUp').show();
            $('#matDown').hide();
        } else {
            $('#matNum').css('color', 'darkseagreen');
            $('#matUp').hide();
            $('#matDown').show();
        }
        cur_mat = matching_time;
        if (repo_time > cur_repo) {
            $('#repNum').css('color', 'indianred');
            $('#repUp').show();
            $('#repDown').hide();
        } else {
            $('#repNum').css('color', 'darkseagreen');
            $('#repUp').hide();
            $('#repDown').show();
        }
        cur_repo = repo_time;
        $('#matNum').text(cur_mat.toFixed(0));
        $('#repNum').text(cur_repo.toFixed(0));

        count += 1;
        acc_reward = data['accu_rewards'];
        if (data['seg_rewards'] > highest_seg_reward) {
            highest_seg_reward = data['seg_rewards'];
        }
        if(data['order_num'] > highest_order){
            highest_order = data['order_num'];
        }
        // avg_seg_reward = (avg_seg_reward * (count - 1) + data['seg_rewards']) / count;
        // avg_order = (avg_order * (count - 1) + data['order_num']) / count;
        // avg_ans_rate = (avg_ans_rate * (count - 1) + data['ans_rate'])/count;
        // avg_rej_rate = (avg_rej_rate * (count - 1) + data['ans_rate']-data['comp_rate'])/count;

        $('#table_accu_rewards').text(acc_reward.toFixed(0));
        $('#table_ans_rate').text(data['ans_rate'].toFixed(2));
        $('#table_avail_driver').text(data['avail_driver_num'].toFixed(0));
        $('#table_order_num').text(data['order_num'].toFixed(0));
        $('#table_rej_rate').text((data['ans_rate'] - data['comp_rate']).toFixed(2));
        $('#table_seg_rewards').text(data['seg_rewards'].toFixed(0));
        $('#table_wait_rate').text((1-data['ans_rate']).toFixed(2));
        $('#table_total_dirver').text(data['total_driver_num'].toFixed(0));
    }
}
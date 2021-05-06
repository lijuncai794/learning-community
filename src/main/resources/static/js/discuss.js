function like(btn, entityType, entityId, entityUserId) {
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType": entityType, "entityId": entityId, "entityUserId": entityUserId},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) { //请求成功
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus == 1 ? '已赞' : "赞");
            } else { //请求失败
                alert(data.msg);
            }
        }
    );
}
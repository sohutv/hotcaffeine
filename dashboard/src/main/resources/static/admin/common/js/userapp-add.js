$("#form-add").validate({
    submitHandler:function(form){
        add();
    }
});

/**
 * 创建app
 */
function add() {
    var appName = $("#appName").val().trim();
    var service = $("#service").val().trim();
    if (!appName) {
        alert("app名称不可为空!");
        return;
    }
    $.ajax({
        cache : true,
        type : "POST",
        url : "/user/app/add",
        data:
            {
                "appName": appName,
                "service": service
            },
        async : false,
        error : function(XMLHttpRequest){
            $.modal.alertError(XMLHttpRequest.responseJSON.msg);
        },
        success : function(data) {
            if (data.code == web_status.SUCCESS) {
                $.modal.alertSuccess(data.msg);
            } else {
                $.modal.alertError(data.msg);
            }
        }
    });
}
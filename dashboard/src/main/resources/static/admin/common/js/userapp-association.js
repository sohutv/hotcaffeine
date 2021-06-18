$("#user-app-association").validate({
    submitHandler : function(form) {
        edit();
    }
});

function edit() {
    if(!$("#apps").val()){
        alert("请选择关联的app");
        return;
    }
    $.ajax({
        cache : false,
        type : "POST",
        url : "/user/app/association",
        data : {
            appName: $('#apps option:selected').val(),
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

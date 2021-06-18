$("#form-edit").validate({
	rules:{
		password:{
			required:true,
			minlength: 5,
			maxlength: 20
		},
		confirm:{
			required:true,
			minlength: 5,
			maxlength: 20,
			equalTo:"#pwd"
		}

	},
	messages: {
		password: {
			required: "请输入新密码",
			minlength: "密码不能小于6个字符",
			maxlength: "密码不能大于20个字符"
		},
		confirm: {
			required: "请再次输入新密码",
			equalTo: "两次密码输入不一致"
		}
	},
	submitHandler:function(form){
		edit();
	}
});


function edit() {
	var dataFormJson=$("#form-edit").serialize();
	$.ajax({
		cache : true,
		type : "POST",
		url :  "/user/editPwd",
		data : dataFormJson,
		async : false,
		error : function(XMLHttpRequest){
			$.modal.alertError(XMLHttpRequest.responseJSON.msg);
		},
		success : function(data) {
			$.operate.saveSuccess(data);
		}
	});
}

<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>Insert title here</title>
</head>
<body>

<form action="upload" method="post" enctype="multipart/form-data">
	file: <input name="FILE_CONTENTS" type="file"/>
	<br/>
	[expected] name: <input name="EXPECTED_NAME"/>
	<br/>
	algorithm: <input name="DIGEST_ALGORITHM"/>
	<br/>
	auth: <input name="AUTHENTICATOR" type="password"/>
	<br/>
	<input type="submit"/>
</form>

</body>
</html>
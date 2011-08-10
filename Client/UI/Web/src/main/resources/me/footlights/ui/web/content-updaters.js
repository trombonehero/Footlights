function updateStatus(date, stat)
{
	var update = document.createElement('p');

	var dateStamp = document.createElement('div');
	dateStamp.appendChild(document.createTextNode(date));
	dateStamp.className = 'timestamp';

	update.appendChild(dateStamp);
	update.appendChild(document.createTextNode(stat));

	var statusRoot = document.getElementById('status');
	statusRoot.insertBefore(update, statusRoot.childNodes[0]);
}


function insertContent(content)
{
	var contentDiv = document.createElement('div');
	contentDiv.className = 'content';
	contentDiv.appendChild(content);

	var contentRoot = document.getElementById('content');
	contentRoot.insertBefore(contentDiv, contentRoot.childNodes[3]);
}


function showAjaxResponse(type, content)
{
	var newcontent = document.createElement('div');
	newcontent.className = 'response';

	var typeDiv = document.createElement('div');
	typeDiv.className = 'type';
	typeDiv.appendChild(document.createTextNode(type));
	newcontent.appendChild(typeDiv);

	var contentDiv = document.createElement('div');
	contentDiv.className = 'content';
	contentDiv.appendChild(document.createTextNode(content));
	newcontent.appendChild(contentDiv);

	insertContent(newcontent);
}


function showError(context, err)
{
	var newcontent = document.createElement('div');
	newcontent.className = 'error';

	var contextDiv = document.createElement('div');
	contextDiv.className = 'ajaxContext';
	contextDiv.appendChild(document.createTextNode(context));
	newcontent.appendChild(contextDiv);

	var contentDiv = document.createElement('div');
	contentDiv.className = 'message';
	contentDiv.appendChild(document.createTextNode(err));
	newcontent.appendChild(contentDiv);

	insertContent(newcontent);
}

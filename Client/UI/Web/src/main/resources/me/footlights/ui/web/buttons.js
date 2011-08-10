function addButtons()
{
	var buttons = document.getElementById('buttons');
	
	buttons.innerHTML='';

	var button = document.createElement('button');
	button.type = 'button';
	button.appendChild(document.createTextNode('Echo'));
	button.onclick = function() { ajax('echo stuff'); }
	buttons.appendChild(button);

	button = document.createElement('button');
	button.type = 'button';
	button.appendChild(document.createTextNode('Run Good Plugin'));
	button.onclick = function() { ajax('run_good'); }
	buttons.appendChild(button);

	button = document.createElement('button');
	button.type = 'button';
	button.appendChild(document.createTextNode('Run Wicked Plugin'));
	button.onclick = function() { ajax('run_evil'); }
	buttons.appendChild(button);

	button = document.createElement('button');
	button.type = 'button';
	button.appendChild(document.createTextNode('Reset'));
	button.onclick = function() { ajax('reset'); }
	buttons.appendChild(button);

	button = document.createElement('button');
	button.type = 'button';
	button.appendChild(document.createTextNode('Shutdown'));
	button.onclick = function() { ajax('shutdown'); }
	buttons.appendChild(button);
}

addButtons();

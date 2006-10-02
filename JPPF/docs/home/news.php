<html>
		<head>
		<title>Java Parallel Processing Framework News Page</title>
		<meta name="description" content="An open-source, Java-based, framework for parallel computing.">
		<meta name="keywords" content="JPPF, Java, Parallel Computing, Distributed Computing, Grid Computing, Cluster, Grid">
		<meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
		<link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
		<link rel="stylesheet" type="text/css" href="./jppf.css" title="Style">
	</head>
	<body>
		<div align="center">
				<table align="center" width="80%" cellspacing="0" cellpadding="5"
			class="table_" style="background: url('images/grid.gif'); background-repeat: repeat; background-attachment: fixed">
			<tr><td height="5"></td></tr>
			<tr>
				<td width="30%" align="left" valign="center">
					<h3>Java Parallel Processing Framework</h3>
				</td>
				<td width="40%" align="center">
					<img src="images/logo.gif" border="0" alt="Java Parallel Processing Framework"/>
				</td>
				<td width="30%" align="right">
					<a href="http://sourceforge.net" target="_top">
						<img src="http://sourceforge.net/sflogo.php?group_id=135654&amp;type=4"
							width="125" height="37" border="0" alt="SourceForge.net Logo" />
					</a>
				</td>
			</tr>
			<tr><td height="5"></td></tr>
		</table>
		<!--<table border="0" style="background-color: #8080FF" cellspacing="0" cellpadding="0" width="80%">-->
		<table style="background: url('images/bkg-menu.gif'); background-repeat: repeat; background-attachment: fixed"
			cellspacing="0" cellpadding="0" width="80%">
			<tr>
				<td>
					<table border="0" cellspacing="0" cellpadding="5">
						<tr>
							<td class="menu_first"><a href="index.html">Home</a></td>
							<td class="menu"><a href="JPPF-Overview.html">Overview</a></td>
							<td class="menu"><a href="http://sourceforge.net/project/showfiles.php?group_id=135654">Files</a></td>
							<td class="menu"><a href="./wiki">Wiki &amp; Doc</a></td>
							<td class="menu"><a href="./forums">Forums</a></td>
							<td class="menu"><a href="screenshots.html">Screenshots</a></td >
							<td class="menu"><a href="api/index.html">API Doc</a></td >
							<td class="menu"><a href="faq.php">Faqs</a></td>
							<td class="menu"><a href="news.php">News</a></td>
							<td class="menu"><a href="http://sourceforge.net/projects/jppf-project">Project</a></td>
							<td class="menu"><a href="links.php">Links</a></td>
							<td class="menu"></td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
		<table cellspacing="0" cellpadding="0" width="80%">
			<tr>
				<td>
								<tr>
				<td class="leftRightBottom_">
					<br>
					<h1 align="center" style="color: #8080FF"><b><a style="color: #8080FF" href="http://sourceforge.net/news/?group_id=135654">Latest news on project pages</a>
</b></h1>
				</td>
			</tr>
				</td>
			</tr>
		</table>
		<?php
		// Connecting, selecting database
		$link = mysql_connect('mysql4-j', 'j135654admin', 'Faz600er')
			 or die('Could not connect: ' . mysql_error());
		mysql_select_db('j135654_web') or die('Could not select database');
		// Performing SQL query
		$query = 'SELECT * FROM news ORDER BY date DESC';
		$result = mysql_query($query) or die('Query failed: ' . mysql_error());
		while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
		{
		?>
		<table class="border_" cellspacing="0" cellpadding="5" width="80%">
			<tr>
				<td>
					<?php
						printf("<h3>%s %s</h3>", date("n/j/Y", strtotime($line["date"])), $line["title"]);
						printf("%s", $line["desc"]);
					?>
						<p><u style="color: #8080FF"><strong style="color: #8080FF">Summary of changes:</strong></u>
					<?php
						printf("%s", $line["content"]);
					?>
				</td>
			</tr>
		</table>
		<?php
		}
		// Free resultset
		mysql_free_result($result);
		// Closing connection
		mysql_close($link);
		?>
	</body>
</html>

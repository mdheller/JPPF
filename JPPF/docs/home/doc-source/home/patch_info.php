<?php $currentPage="Patches" ?>
$template{name="about-page-header" title="Patches"}$
<?php
	$patch_id = $_REQUEST["patch_id"];
	// Connecting, selecting database
	$link = mysql_connect('localhost', 'lolocohe_jppfadm', 'tri75den')
		 or die('Could not connect: ' . mysql_error());
	mysql_select_db('lolocohe_jppfweb') or die('Could not select database');

	// Performing SQL query
	$query = "SELECT * FROM patch where id = " . $patch_id;
	$result = mysql_query($query) or die('Query failed: ' . mysql_error());
	$line = mysql_fetch_array($result, MYSQL_ASSOC);
	$jppf_ver = $line["jppf_version"];
	$patch_number = $line["patch_number"];
	$patch_url = $line["patch_url"];
	$readme = preg_replace('@\n@', '<br/>', $line['readme']);
	$readme = preg_replace('@(^ )+@', '&nbsp;', $readme);
?>
	<h1>JPPF <?php echo $jppf_ver ?> patch <?php echo $patch_number ?></h1>
	<h3>Download:</h3>
<?php
	$port = ($_SERVER['SERVER_PORT'] == 80) ? '' : ':' . $_SERVER['SERVER_PORT'];
?>
	<a href="<?php echo $patch_url ?>"><?php echo 'http://' . $_SERVER['SERVER_NAME'] . $port . $patch_url ?></a>
	<h3>Description (included readme.txt):</h3>
	<?php echo preg_replace('/\n/', '<br/>', $line['readme']) ?>
	<h3>Fixed bugs:</h3>
	<ul class="samplesList">
<?php
	mysql_free_result($result);
	$query = "SELECT * FROM patch_bugs where jppf_version = '" . $jppf_ver . "' AND patch_number = '" . $patch_number . "'";
	$result = mysql_query($query) or die('Query failed: ' . mysql_error());
	while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
	{
?>
		<li><a href="<?php echo $line['bug_url'] ?>"><?php echo $line['bug_id'] ?>&nbsp;<?php echo $line['bug_title'] ?></a></li>
<?php
	}
?>
	</ul>
<?php
	// Free resultset
	mysql_free_result($result);
	// Closing connection
	mysql_close($link);
?>
$template{name="about-page-footer"}$
<?php

$temperature = $_POST['temperature']; 
$humidity = $_POST['humidity'];

$dbc = mysqli_connect("localhost", "brian", "password", "mydb") or die("Bad Connnect: " .mysqli_connect_error());
$sql = "insert into temp_humidity_data (temperature, humidity) values ('$temperature', '$humidity')";
$result = mysqli_query($dbc, $sql) or die("Bad Query");
echo "Record inserted successfully"; 

?>

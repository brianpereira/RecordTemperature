<?php
 
// Create connection
$con=mysqli_connect("localhost","brian","password","mydb");
 
// Check connection
if (mysqli_connect_errno())
{
  echo "Failed to connect to MySQL: " . mysqli_connect_error();
}
 
// Select data from table 'temp_humidity_data'
$sql = "SELECT datetime, temperature, humidity FROM temp_humidity_data";
 
// Confirm there are results
if ($result = mysqli_query($con, $sql))
{
	// We have results, create an array to hold the results
        // and an array to hold the data
	$resultArray = array();
	$tempArray = array();
 
	// Loop through each result
	while($row = $result->fetch_object())
	{
		// Add each result into the results array
		$tempArray = $row;
	    array_push($resultArray, $tempArray);
	}
 
	// Encode the array to JSON and output the results
	echo json_encode($resultArray);
}
 
// Close connections
mysqli_close($con);
?>

on run {msgText, targetPhoneNum}
	tell application "Messages"
		set serviceID to id of 1st service whose service type = iMessage
		send msgText to buddy targetPhoneNum of service id serviceID
	end tell
end run
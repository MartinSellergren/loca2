USER_NAME = masel
TOKEN = sk.eyJ1IjoibWFzZWwiLCJhIjoiY2pkM2YwYXlvMncyZTMzczYzbXJ1Mzd3ayJ9.OBMENh5mWq6iwPZut8s0iw
BASE_MAP_ID = streets-v10
LOCA_DEFAULT_MAP_ID = cjkyl6he503zy2rptne68047q

all: clean fetch generate update

#fetch Streets.json
fetch:
	curl "https://api.mapbox.com/styles/v1/mapbox/$(BASE_MAP_ID)?access_token=$(TOKEN)" > Streets.json

#generate styles from Streets.json
generate:
	python styleCreator.py Streets.json

update:
	curl -X PATCH "https://api.mapbox.com/styles/v1/$(USER_NAME)/$(LOCA_DEFAULT_MAP_ID)?access_token=$(TOKEN)" --data @loca-default-map-style.json --header "Content-Type:application/json"

#remove generated
clean:
	rm -f *.json *.pyc

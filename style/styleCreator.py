import json
import sys
import colorsys


def removeCreatedAndModifiedProps(data):
    if 'created' in data:
        del data['created']
    if 'modified' in data:
        del data['modified']

def setOwnerAndVisibility(data):
    data['owner'] = 'masel'
    data['visibility'] = 'private'

def setDefaultViewport(data):
    data['center'] = [0, 0]
    data['zoom'] = 0


# def getLayersFromSourceLayer(data, sourceLayers):
#     filtered = []
#     for layer in data['layers']:
#         if 'source-layer' in layer and layer['source-layer'] in sourceLayers:
#             filtered.append(layer)
#     return filtered

# def getLabelLayers(data):
#     return getLayersFromSourceLayer(data, ['country_label',
#                                            'marine_label',
#                                            'state_label',
#                                            'place_label',
#                                            'water_label',
#                                            'poi_label',
#                                            'road_label',
#                                            'waterway_label',
#                                            'airport_label',
#                                            'rail_station_label',
#                                            'mountain_peak_label'])

def getLayersFromTypes(data, types):
    filtered = []
    for layer in data['layers']:
        if 'type' in layer and layer['type'] in types:
            filtered.append(layer)
    return filtered

def getSymbolLayers(data):
    return getLayersFromTypes(data, ['symbol'])

def removeAllLabels(data):
    for layer in getSymbolLayers(data):
        data['layers'].remove(layer)

def setName(data, name):
    data['name'] = name

def dumpStyle(data, fileName):
    f = open(fileName + ".json", 'w')
    f.write(json.dumps(data, indent=4))
    f.close()



if __name__ == '__main__':
    data = json.load(open(sys.argv[1], 'r'))

    removeCreatedAndModifiedProps(data)
    setOwnerAndVisibility(data)
    setDefaultViewport(data)

    removeAllLabels(data)

    name = 'loca-default-map-style'
    setName(data, name)
    dumpStyle(data, name)

[out:xml][timeout:900][bbox:{{bbox}}];

node[name];
out qt;

way[name];
out qt ({{bbox}}) geom;
convert WAY _id=id(), length=length(), is_closed=is_closed();
out qt noids;

rel[name];
map_to_area; rel(pivot);
convert REL ::id=id(), ::geom=hull(geom()), ::=::, version=version(), no_members=count_members();
out qt geom;

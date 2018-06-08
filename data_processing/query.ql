[out:xml][timeout:900][bbox:{{bbox}}];

node[name];
out qt meta;

way[name];
foreach {
   (._; convert WAY ::id=id(), version=version(), length=length(), is_closed=is_closed(););
   out qt ({{bbox}}) geom;
}

rel[name];
map_to_area; rel(pivot);
convert REL ::id=id(), ::geom=hull(geom()), ::=::, version=version(), no_members=count_members();
out qt geom;

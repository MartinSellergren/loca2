[out:xml][timeout:900][bbox:{{bbox}}];

node[name];
convert NODE ::id=id(), ::geom=geom(), version=version(), ::=::;
out qt geom;

way[name](if: is_closed());
convert WAY ::id=id(), ::geom=hull(geom()), version=version(), ::=::;
out qt geom;

way[name](if: !is_closed());
convert WAY ::id=id(), ::geom=trace(geom()), version=version(), ::=::;
out qt geom;

rel[name];
map_to_area; rel(pivot);
convert REL ::id=id(), ::geom=hull(geom()), version=version(), ::=::;
out qt geom;

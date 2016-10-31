Ext.require([ 'Ext.tree.*', 'Ext.data.*' ]);

Ext.define('Document', {
	extend : 'Ext.data.Model',
	fields : [ {
		name : 'id',
		type : 'string'
	}, {
		name : 'name',
		type : 'string'
	} ]
});

var docStore = new Ext.data.TreeStore({
	model : 'Document',
	proxy : {
		type : 'ajax',
		url : '/childs'
	}
});

Ext.onReady(function() {

	new Ext.panel.Panel({
		renderTo : 'tree-div',
		width : 300,
		height : 200,
		layout : {
			type : 'hbox',
			align : 'stretch'
		},
		defaultType : 'treepanel',
		defaults : {
			rootVisible : false,
			flex : 1
		},
		items : [ {
			title : 'Source',
			store : docStore,
			viewConfig : {
				plugins : {
					ptype : 'treeviewdragdrop',
					enableDrag : true,
					enableDrop : false
				}
			}
		} ]
	});
});

package com.example.newyorkweather

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.example.newyorkweather.data.GeoResult

class GeoSuggestionAdapter(
    context: Context,
    resource: Int
) : ArrayAdapter<String>(context, resource) {

    private var suggestionStrings: List<String> = emptyList()
    private var geoResults: List<GeoResult?> = emptyList()

    fun updateSuggestions(displayStrings: List<String>, results: List<GeoResult?>) {
        suggestionStrings = displayStrings
        geoResults = results
        clear()
        addAll(displayStrings)
        notifyDataSetChanged()
    }

    fun getGeoResult(position: Int): GeoResult? {
        return if (position in geoResults.indices) geoResults[position] else null
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return FilterResults().apply {
                    values = suggestionStrings
                    count = suggestionStrings.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}

package com.example.microjob.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.microjob.model.Category

/**
 * Horizontal row of category chips. Tapping a chip filters the job list;
 * tapping the selected chip again clears the filter.
 */
@Composable
fun CategoryRow(
    categories: List<Category>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val selected = selectedCategory == category.name
            FilterChip(
                selected = selected,
                onClick = { onCategorySelect(if (selected) null else category.name) },
                label = {
                    Row {
                        Text(category.emoji)
                        Text(
                            text = " ${category.name}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            )
        }
    }
}

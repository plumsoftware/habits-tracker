package ru.plumsoftware.habitstracker.ui.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.androidx.compose.koinViewModel
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.ui.components.BottomBar
import ru.plumsoftware.habitstracker.ui.components.CalendarView
import ru.plumsoftware.habitstracker.ui.components.DayItem
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeEvent
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeViewModel
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen() {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomBar()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimen.medium)
                .padding(top = Dimen.large),
            verticalArrangement = Arrangement.spacedBy(
                space = Dimen.medium,
                alignment = Alignment.Top
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize(),
                    verticalArrangement = Arrangement.spacedBy(
                        space = Dimen.medium,
                        alignment = Alignment.Top
                    ),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = getMonthTitle(state.selectedDate),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "Привычки на сегодня",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Card(
                    modifier = Modifier.size(44.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.elevatedCardElevation(0.dp, 0.dp, 0.dp),
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            modifier = Modifier.align(Alignment.Center),
                            painter = painterResource(R.drawable.calendar_filled),
                            contentDescription = null
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(Dimen.medium)
            )

            CalendarView(
                selectedDate = state.selectedDate,
                selectedDay = state.selectedDay,
                onDaySelected = { day ->
                    viewModel.onEvent(HomeEvent.SelectDay(day))
                }
            )
        }
    }
}

@Composable
fun getMonthTitle(selectedDate: Long): String {
    val cal = Calendar.getInstance().apply {
        timeInMillis = selectedDate
    }
    val monthFormat = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
    return monthFormat.format(cal.time).replaceFirstChar { it.uppercase() }
}

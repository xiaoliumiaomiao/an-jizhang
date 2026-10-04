package com.xiaoliumiaomiao.anjizhang.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.YearMonth

@Composable internal fun DestinationIcon(key:String) {
    val color=MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(24.dp)) {
        val w=size.width; val h=size.height; val stroke=2.dp.toPx()
        when(key) {
            "ledger" -> listOf(.25f,.5f,.75f).forEach { y->
                drawCircle(color,stroke/2,Offset(w*.15f,h*y))
                drawLine(color,Offset(w*.35f,h*y),Offset(w*.9f,h*y),stroke)
            }
            "stats" -> listOf(.3f,.65f,.9f).forEachIndexed { i,height->
                val x=w*(.2f+i*.3f);drawLine(color,Offset(x,h*.9f),Offset(x,h*(1-height)),stroke*2)
            }
            else -> {
                drawCircle(color,w*.3f,center,style=Stroke(stroke))
                drawCircle(color,w*.08f,center)
                listOf(Offset(w*.5f,0f) to Offset(w*.5f,h*.2f),Offset(w*.5f,h*.8f) to Offset(w*.5f,h),Offset(0f,h*.5f) to Offset(w*.2f,h*.5f),Offset(w*.8f,h*.5f) to Offset(w,h*.5f)).forEach{(a,b)->drawLine(color,a,b,stroke)}
            }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun MonthNavigator(month:YearMonth,onChange:(YearMonth)->Unit) {
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        TextButton(enabled=month>YearMonth.of(1900,1),onClick={onChange(month.minusMonths(1))}){Text("上月")}
        Text("${month.year}年${month.monthValue}月",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(vertical=12.dp))
        TextButton(enabled=month<YearMonth.of(9999,12),onClick={onChange(month.plusMonths(1))}){Text("下月")}
        TextButton(onClick={onChange(YearMonth.now())}){Text("本月")}
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun MonthlyOverview(summary:MonthlySummary) {
    Column(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("整月概览",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("收入 ${Money.format(summary.incomeCents)}",color=MaterialTheme.colorScheme.secondary)
            Text("支出 ${Money.format(summary.expenseCents)}",color=MaterialTheme.colorScheme.error)
        }
        Text("本月结余 ${Money.format(summary.balanceCents)}",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
    }
}
@Composable internal fun LedgerPage(month:YearMonth,summary:MonthlySummary,visible:List<Entry>,total:Int,query:String,kind:EntryKind?,busy:Boolean,listState:LazyListState,onMonth:(YearMonth)->Unit,onQuery:(String)->Unit,onKind:(EntryKind?)->Unit,onClear:()->Unit,onAdd:()->Unit,onDetail:(Entry)->Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("ledger-list"),state=listState,contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=96.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { MonthNavigator(month,onMonth) }
        item { MonthlyOverview(summary) }
        item { EntryFilters(query,kind,onQuery,onKind,onClear) }
        item { Text("显示 ${visible.size} / 本月 $total 条",style=MaterialTheme.typography.bodySmall) }
        if(total==0) item {
            Column(Modifier.fillMaxWidth().padding(vertical=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("这个月还没有账目",style=MaterialTheme.typography.titleMedium)
                Button(onClick=onAdd,enabled=!busy){Text("记一笔")}
            }
        } else if(visible.isEmpty()) item { Text("没有符合条件的账目",style=MaterialTheme.typography.titleMedium) }
        EntryGroups.forDisplay(visible).forEach { day->
            item(key="day-${day.date}") { Text(day.date.toString(),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp)) }
            items(day.entries,key={it.id}) { entry-> EntryRow(entry,busy){onDetail(entry)} }
        }
    }
}
@Composable private fun EntryRow(entry:Entry,busy:Boolean,onClick:()->Unit) {
    Column(Modifier.fillMaxWidth().testTag("entry-${entry.id}").clickable(enabled=!busy,onClick=onClick).padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val availableWidth=maxWidth
            val amount=(if(entry.kind==EntryKind.EXPENSE)"-" else "+")+Money.format(entry.amountCents)
            val amountColor=if(entry.kind==EntryKind.EXPENSE)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
            if(LocalDensity.current.fontScale<1.5f && maxWidth>=320.dp) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(entry.category,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f),maxLines=2,overflow=TextOverflow.Ellipsis)
                    Text(amount,style=MaterialTheme.typography.titleLarge,textAlign=TextAlign.End,modifier=Modifier.widthIn(max=availableWidth*.6f),color=amountColor)
                }
            } else Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(entry.category,style=MaterialTheme.typography.titleMedium)
                Text(amount,style=MaterialTheme.typography.titleLarge,textAlign=TextAlign.End,modifier=Modifier.fillMaxWidth(),color=amountColor)
            }
        }
        Text(if(entry.kind==EntryKind.EXPENSE)"支出" else "收入",style=MaterialTheme.typography.bodySmall)
        if(entry.note.isNotEmpty()) Text(entry.note,maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium)
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    }
}
@Composable internal fun StatisticsPage(month:YearMonth,summary:MonthlySummary,listState:LazyListState,onMonth:(YearMonth)->Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("stats-list"),state=listState,contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=96.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { MonthNavigator(month,onMonth) }; item { MonthlyOverview(summary) }
        item { Text("支出分类",style=MaterialTheme.typography.titleLarge) }
        if(summary.expensesByCategory.isEmpty()) item { Text("这个月还没有支出") }
        items(summary.expensesByCategory.toList().sortedByDescending{it.second},key={it.first}) { (category,cents)->
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("$category  ${Money.format(cents)}")
                LinearProgressIndicator(progress={cents.toFloat()/summary.expenseCents.toFloat()},modifier=Modifier.fillMaxWidth())
            }
        }
    }
}
@Composable internal fun SettingsPage(busy:Boolean,listState:LazyListState,onExport:()->Unit,onImport:()->Unit,onLicenses:()->Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("settings-list"),state=listState,contentPadding=PaddingValues(start=16.dp,end=16.dp,top=16.dp,bottom=96.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Text("备份与恢复",style=MaterialTheme.typography.titleLarge) }
        item { Button(onClick=onExport,enabled=!busy){Text("导出备份")}; OutlinedButton(onClick=onImport,enabled=!busy){Text("恢复备份")} }
        item { Text("备份文件包含明文账目，请保存在可信位置。恢复会合并账目，相同记录跳过；内容冲突时整批拒绝。") }
        item { Text("整本备份最多50000条且不超过10 MiB，目前不支持分批导出。备注等内容也占用容量；接近上限时请先保存备份。") }
        item { HorizontalDivider();Text("本地隐私",style=MaterialTheme.typography.titleLarge) }
        item { Text("账目仅保存在此设备，无需登录，不联网，无广告或分析服务。") }
        item { Text("卸载提醒",style=MaterialTheme.typography.titleLarge);Text("卸载应用会删除本地账目；更换设备或卸载前，请先导出备份。系统云备份已关闭。") }
        item { HorizontalDivider();Text("关于",style=MaterialTheme.typography.titleLarge);Text("版本 0.1.3");TextButton(onClick=onLicenses){Text("第三方许可")} }
    }
}
@Composable internal fun EntryDetail(entry:Entry,busy:Boolean,onEdit:()->Unit,onDelete:()->Unit) {
    Column(Modifier.fillMaxSize().testTag("entry-detail").verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text((if(entry.kind==EntryKind.EXPENSE)"-" else "+")+Money.format(entry.amountCents),style=MaterialTheme.typography.headlineMedium)
        Text(if(entry.kind==EntryKind.EXPENSE)"支出" else "收入")
        Text("分类：${entry.category}");Text("日期：${entry.date}")
        Text("备注：${entry.note.ifEmpty{"无"}}")
        Button(onClick=onEdit,enabled=!busy){Text("编辑")}
        OutlinedButton(onClick=onDelete,enabled=!busy){Text("删除")}
    }
}

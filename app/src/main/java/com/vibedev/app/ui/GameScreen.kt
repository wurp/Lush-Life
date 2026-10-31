package com.vibedev.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibedev.app.life.Edges
import com.vibedev.app.life.Rule
import kotlin.math.roundToInt

private enum class Sheet { PATTERNS, THEMES, RULES, SETTINGS }

@Composable
fun GameScreen(controller: LifeController) {
    val theme = controller.theme
    MaterialTheme(colorScheme = schemeFor(theme)) {
        val viewport = remember { Viewport() }
        var sheet by remember { mutableStateOf<Sheet?>(null) }
        var showInfo by remember { mutableStateOf(false) }
        var showControls by remember { mutableStateOf(true) }

        RunLoop(controller)

        Box(Modifier.fillMaxSize().background(theme.background)) {
            LifeCanvas(controller, viewport, Modifier.fillMaxSize())

            InfoPill(
                controller,
                onClick = { showInfo = true },
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp),
            )

            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp)) {
                if (showControls) {
                    ControlPanel(
                        controller,
                        onFit = { viewport.fit(controller.world) },
                        onOpen = { sheet = it },
                        onHide = { showControls = false },
                    )
                } else {
                    RoundButton("☰", null, onClick = { showControls = true }, modifier = Modifier.align(Alignment.End))
                }
            }
        }

        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text(controller.pattern.name) },
                text = { Text(controller.pattern.description + "\n\nRule: ${controller.rule.name} (${controller.rule.code})") },
                confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } },
            )
        }

        if (controller.sizeWarning) SizeWarningDialog(controller)

        sheet?.let { current ->
            ChooserSheet(current, controller, onClose = { sheet = null })
        }
    }
}

private fun schemeFor(theme: LifeTheme) = if (theme.isLight) {
    lightColorScheme(
        primary = theme.accent, onPrimary = Color.White,
        surface = theme.background, background = theme.background,
        surfaceContainerLow = theme.trail, surfaceContainer = theme.trail,
    )
} else {
    darkColorScheme(
        primary = theme.accent, onPrimary = theme.background,
        surface = theme.background, background = theme.background,
        surfaceContainerLow = theme.trail, surfaceContainer = theme.trail,
        secondaryContainer = theme.trail,
    )
}

/** Advances the world at the chosen speed while it's playing. */
@Composable
private fun RunLoop(controller: LifeController) {
    LaunchedEffect(controller.running) {
        if (!controller.running) return@LaunchedEffect
        var last = 0L
        var due = 0.0
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) due += (now - last) / 1e9 * controller.speed
                last = now
                val n = due.toInt()
                if (n > 0) {
                    due -= controller.stepMany(minOf(n, 10), budgetNanos = 20_000_000L)
                    // If the world is too big to keep up, run as fast as possible instead of piling up work.
                    if (due > 10) due = 0.0
                }
            }
        }
    }
}

@Composable
private fun InfoPill(controller: LifeController, onClick: () -> Unit, modifier: Modifier = Modifier) {
    controller.frame // Refresh counters every generation.
    Surface(
        modifier = modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        color = controller.theme.background.copy(alpha = 0.75f),
        shape = RoundedCornerShape(50),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${controller.pattern.name}  ⓘ",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Gen ${controller.world.generation}  ·  ${controller.world.population} alive",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ControlPanel(
    controller: LifeController,
    onFit: () -> Unit,
    onOpen: (Sheet) -> Unit,
    onHide: () -> Unit,
) {
    Surface(
        color = controller.theme.background.copy(alpha = 0.82f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, controller.theme.grid, RoundedCornerShape(24.dp)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundButton("↺", "Restart", onClick = controller::restart)
                RoundButton("+1", "Step", onClick = { controller.running = false; controller.step() })
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledIconButton(
                        onClick = { controller.running = !controller.running },
                        modifier = Modifier.size(60.dp),
                    ) { Text(if (controller.running) "❚❚" else "▶", fontSize = 24.sp) }
                    Caption(if (controller.running) "Pause" else "Play")
                }
                RoundButton(
                    if (controller.drawMode) "✎" else "✋",
                    if (controller.drawMode) "Draw" else "Move",
                    selected = controller.drawMode,
                    onClick = { controller.drawMode = !controller.drawMode },
                )
                RoundButton("⤢", "Fit", onClick = onFit)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Speed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(48.dp),
                )
                Slider(
                    value = controller.speed.toFloat(),
                    onValueChange = { controller.changeSpeed(it.roundToInt()) },
                    valueRange = 1f..120f,
                    modifier = Modifier.weight(1f).height(32.dp),
                )
                Text(
                    "${controller.speed}/s",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp).width(44.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MenuButton("Patterns", Modifier.weight(1f)) { onOpen(Sheet.PATTERNS) }
                MenuButton("Themes", Modifier.weight(1f)) { onOpen(Sheet.THEMES) }
                MenuButton("Rules", Modifier.weight(1f)) { onOpen(Sheet.RULES) }
                MenuButton("⚙", Modifier.width(48.dp)) { onOpen(Sheet.SETTINGS) }
                MenuButton("⌄", Modifier.width(44.dp), onHide)
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
}

@Composable
private fun RoundButton(
    label: String,
    caption: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        RoundButtonCore(label, onClick, selected)
        caption?.let { Caption(it) }
    }
}

@Composable
private fun RoundButtonCore(label: String, onClick: () -> Unit, selected: Boolean) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        ),
    ) { Text(label, fontSize = 20.sp) }
}

@Composable
private fun MenuButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
    ) { Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooserSheet(sheet: Sheet, controller: LifeController, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        when (sheet) {
            Sheet.PATTERNS -> PatternList(controller, onClose)
            Sheet.THEMES -> ThemeList(controller, onClose)
            Sheet.RULES -> RuleList(controller, onClose)
            Sheet.SETTINGS -> SettingsPanel(controller)
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun ListRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit, leading: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let { it(); Box(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 3)
        }
    }
}

@Composable
private fun PatternList(controller: LifeController, onClose: () -> Unit) {
    val groups = controller.library.all.groupBy { it.category }
    LazyColumn(Modifier.padding(bottom = 24.dp)) {
        item { SheetTitle("Start patterns") }
        groups.forEach { (category, patterns) ->
            item {
                Text(
                    category.title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
                )
            }
            items(patterns, key = { it.name }) { p ->
                ListRow(p.name, p.description.substringBefore("\n"), p == controller.pattern, onClick = {
                    controller.load(p)
                    onClose()
                })
            }
        }
    }
}

@Composable
private fun ThemeList(controller: LifeController, onClose: () -> Unit) {
    LazyColumn(Modifier.padding(bottom = 24.dp)) {
        item { SheetTitle("Themes") }
        items(LifeThemes.all, key = { it.name }) { t ->
            ListRow(t.name, if (t.isLight) "Light" else "Dark", t == controller.theme, onClick = {
                controller.chooseTheme(t)
                onClose()
            }, leading = {
                Box(
                    Modifier
                        .size(width = 72.dp, height = 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(t.background)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.horizontalGradient(t.ages)),
                )
            })
        }
    }
}

@Composable
private fun RuleList(controller: LifeController, onClose: () -> Unit) {
    LazyColumn(Modifier.padding(bottom = 24.dp)) {
        item { SheetTitle("Rules") }
        items(Rule.presets, key = { it.code }) { r ->
            ListRow("${r.name}  ·  ${r.code}", r.description, r.code == controller.rule.code, onClick = {
                controller.chooseRule(r)
                onClose()
            })
        }
        item {
            Text(
                "Computer parts and spaceships only work with Conway's Life.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsPanel(controller: LifeController) {
    Column(Modifier.padding(bottom = 32.dp)) {
        SheetTitle("Settings")
        SwitchRow("Fading trails", "Dead cells leave a glowing trail", controller.trails, controller::changeTrails)
        SwitchRow("Grid lines", "Shown when zoomed in", controller.gridLines, controller::changeGridLines)
        Text(
            "Edges",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
        )
        FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Edges.entries.forEach { e ->
                FilterChip(selected = e == controller.edges, onClick = { controller.changeEdges(e) }, label = { Text(e.label) })
            }
        }
        Text(
            controller.edges.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 20.dp))
        Text(
            "Starting world size (restarts the pattern)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorldSize.entries.forEach { s ->
                FilterChip(
                    selected = s == controller.worldSize,
                    onClick = { controller.changeWorldSize(s) },
                    label = { Text("${s.label} ${s.width}×${s.height}") },
                )
            }
        }
        TextButton(onClick = controller::clear, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("Clear the world (draw your own)")
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SizeWarningDialog(controller: LifeController) {
    val world = controller.world
    AlertDialog(
        onDismissRequest = controller::stopSimulating,
        title = { Text("The world is getting very large") },
        text = {
            Column {
                Text(
                    "It's now ${world.width} × ${world.height} cells and still growing, so it may start to slow down. What would you like to do?",
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TextButton(onClick = controller::useHardEdges) { Text("Hard edges here: things drop off") }
                TextButton(onClick = controller::useWrap) { Text("Wrap around at this size") }
                TextButton(onClick = controller::keepGrowing) { Text("Keep growing") }
                TextButton(onClick = controller::stopSimulating) { Text("Stop simulating") }
            }
        },
        confirmButton = {},
    )
}

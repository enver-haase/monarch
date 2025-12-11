package com.infraleap.examplefeature.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Route("")
public class MonarchView extends VerticalLayout {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final String[] smileys = {"😀", "😁", "😂", "😅", "😆", "😉", "😊"};
    private boolean isAnimating = false;

    private int cashInCents = 10000;
    private final Span cashDisplay = new Span();
    private int skipRounds = 0;

    public MonarchView() {
        setHeight("100vh");
        setAlignItems(Alignment.CENTER);
        addClassName("marble-background");

        H1 title = new H1("MONARCH");
        title.getStyle().set("font-size", "4rem");
        title.getStyle().set("margin", "1rem 0");
        title.getStyle().set("font-family", "'Cinzel Decorative', serif");
        title.getStyle().set("font-weight", "900");
        title.addClassName("fire-text");

        HorizontalLayout gridsLayout = new HorizontalLayout();
        gridsLayout.setWidthFull();
        gridsLayout.setHeight("66vh"); // 2/3 of the screen height
        gridsLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.STRETCH);

        Grid<String> grid1 = createSmileyGrid(List.of(smileys));
        Grid<String> grid2 = createSmileyGrid(List.of(smileys));
        Grid<String> grid3 = createSmileyGrid(List.of(smileys));
        Grid<String>[] grids = new Grid[]{grid1, grid2, grid3};

        gridsLayout.add(grid1, grid2, grid3);

        // Setup cash display footer
        cashDisplay.addClassName("cash-display");
        updateCashDisplay();

        add(title, gridsLayout, cashDisplay);

        // Start background worker thread that executes every 1 second
        UI ui = UI.getCurrent();
        scheduler.scheduleAtFixedRate(() -> {
            ui.access(() -> {
                // Check if we need to skip this round
                if (skipRounds > 0) {
                    skipRounds--;
                    if (skipRounds == 0) {
                        // Remove flash animation after skip period ends
                        removeClassName("winner-flash");
                    }
                    return;
                }

                // Play the game if there's enough cash
                if (cashInCents >= 20) {
                    // Skip if previous animation still running
                    if (isAnimating) {
                        return;
                    }

                    cashInCents -= 20;
                    isAnimating = true;

                    // Store selected smileys for winner detection
                    final String[] selectedSmileys = new String[3];

                    for (int i = 0; i < grids.length; i++) {
                        Grid<String> grid = grids[i];
                        int index = (int) (Math.random() * smileys.length);
                        String selectedSmiley = smileys[index];
                        selectedSmileys[i] = selectedSmiley;

                        // Don't call select() - Grid auto-scrolls to selected items which interferes with animation
                        animateGridSpin(grid, selectedSmiley, i);
                    }

                    // Schedule winner detection after animations complete
                    scheduler.schedule(() -> {
                        ui.access(() -> {
                            isAnimating = false;

                            // Check if all three match
                            if (selectedSmileys[0].equals(selectedSmileys[1]) &&
                                selectedSmileys[1].equals(selectedSmileys[2])) {
                                cashInCents += 2500;
                                System.out.println("We have a WINNER! Smiley: " + selectedSmileys[0] + ", New Cash: " + cashInCents + " cents");
                                updateCashDisplay();
                                addClassName("winner-flash");
                                skipRounds = 3;
                            } else {
                                System.out.println("No winner this round. Cash remaining: " + cashInCents + " cents");
                                updateCashDisplay();
                            }
                        });
                    }, 1600, TimeUnit.MILLISECONDS); // Wait for longest animation (1400ms) + buffer
                }
            });
        }, 4, 4, TimeUnit.SECONDS); // Spin every 4 seconds to allow time to see the animation

        // Cleanup scheduler when component is detached
        addDetachListener(event -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
    }

    private void animateGridSpin(Grid<String> grid, String targetSmiley, int gridIndex) {
        int targetIndex = Arrays.asList(smileys).indexOf(targetSmiley);

        grid.getElement().executeJs(
            // JavaScript animation code
            "const grid = this; " +
            "const targetIndex = $0; " +
            "const gridIndex = $1; " +

            // Independent timing based on grid index
            "const spinDuration = 800 + (gridIndex * 300); " + // 800ms, 1100ms, 1400ms
            "const spinCycles = 3 + gridIndex; " + // 3, 4, 5 full cycles

            "const table = grid.shadowRoot.getElementById('table'); " +
            "if (!table) { console.error('Table not found'); return; } " +

            // Calculate actual row height from first row (try multiple selectors)
            "let firstRow = table.querySelector('tbody tr'); " +
            "if (!firstRow) firstRow = table.querySelector('tr'); " +
            "if (!firstRow) { " +
            "  console.error('No rows found in table'); " +
            "  console.log('Table HTML:', table.innerHTML.substring(0, 500)); " +
            "  return; " +
            "} " +
            "const itemHeight = firstRow.offsetHeight; " +
            "console.log('Row height:', itemHeight, 'px', 'Row:', firstRow); " +

            // Enable scrolling and hide scrollbars
            "table.style.overflow = 'auto'; " +
            "table.style.scrollbarWidth = 'none'; " + // Firefox
            "table.style.msOverflowStyle = 'none'; " + // IE/Edge
            // Inject WebKit scrollbar hiding if not already present
            "if (!grid.shadowRoot.querySelector('#webkit-scrollbar-hide')) { " +
            "  const style = document.createElement('style'); " +
            "  style.id = 'webkit-scrollbar-hide'; " +
            "  style.textContent = '#table::-webkit-scrollbar { display: none !important; }'; " +
            "  grid.shadowRoot.appendChild(style); " +
            "} " +
            "console.log('Overflow set to auto, scrollHeight:', table.scrollHeight); " +

            "const startTime = Date.now(); " +
            "const totalItems = 7; " +
            "const maxScroll = table.scrollHeight - table.offsetHeight; " +
            "console.log('maxScroll:', maxScroll, 'itemHeight:', itemHeight); " +

            // Calculate total distance to travel (with cycles)
            // Use maxScroll for cycles to match wrapping behavior
            "const targetFinalIndex = targetIndex; " +
            "const totalDistance = (spinCycles * maxScroll) + (targetFinalIndex * itemHeight); " +
            "console.log('Total distance to travel:', totalDistance, 'cycles:', spinCycles); " +

            // Easing function (ease-in-out-cubic) - starts slow, fast in middle, ends slow
            "function easeInOutCubic(t) { " +
            "  return t < 0.5 " +
            "    ? 4 * t * t * t " + // Ease in (accelerate)
            "    : 1 - Math.pow(-2 * t + 2, 3) / 2; " + // Ease out (decelerate)
            "} " +

            // Animation loop with wrapping
            "function animate() { " +
            "  const elapsed = Date.now() - startTime; " +
            "  const progress = Math.min(elapsed / spinDuration, 1); " +
            "  const easedProgress = easeInOutCubic(progress); " +
            "  " +
            "  const virtualScroll = totalDistance * easedProgress; " + // Calculate position in virtual scroll space
            "  const wrappedScroll = virtualScroll % maxScroll; " + // Wrap to actual scrollable range
            "  " +
            "  table.scrollTop = wrappedScroll; " +
            "  " +
            "  if (progress < 1) { " +
            "    requestAnimationFrame(animate); " +
            "  } else { " +
            "    table.scrollTop = targetFinalIndex * itemHeight; " + // Final position - no modulo needed
            "    console.log('Animation complete, final scroll:', table.scrollTop); " +
            "  } " +
            "} " +
            "" +
            "requestAnimationFrame(animate);",
            targetIndex,
            gridIndex
        );
    }

    private Grid<String> createSmileyGrid(List<String> smileys) {
        Grid<String> grid = new Grid<>();
        grid.setItems(smileys);
        grid.addColumn(smiley -> smiley).setHeader("Smiley");
        grid.setHeightFull();
        grid.setWidth("33%");
        grid.addClassName("large-rows");

        // Hide scrollbars but allow scrolling - apply immediately on attach
        grid.addAttachListener(event -> {
            grid.getElement().executeJs(
                "const grid = this; " +
                "const table = grid.shadowRoot.getElementById('table'); " +
                "if (table) { " +
                "  table.style.overflow = 'auto'; " +
                "  table.style.scrollbarWidth = 'none'; " + // Firefox
                "  table.style.msOverflowStyle = 'none'; " + // IE/Edge
                "  if (!grid.shadowRoot.querySelector('#initial-scrollbar-hide')) { " +
                "    const style = document.createElement('style'); " +
                "    style.id = 'initial-scrollbar-hide'; " +
                "    style.textContent = '#table::-webkit-scrollbar { display: none !important; }'; " +
                "    grid.shadowRoot.appendChild(style); " +
                "  } " +
                "  console.log('Initial scrollbar hiding applied'); " +
                "}"
            );
        });

        return grid;
    }

    private void updateCashDisplay() {
        double dollars = cashInCents / 100.0;
        cashDisplay.setText(String.format(Locale.US, "$%.2f", dollars));
    }
}

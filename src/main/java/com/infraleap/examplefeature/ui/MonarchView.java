package com.infraleap.examplefeature.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Route("")
public class MonarchView extends VerticalLayout {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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

        String[] smileys = {"😀", "😁", "😂", "😅", "😆", "😉", "😊"};
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
                    cashInCents -= 20;
                    for (Grid<String> grid : grids) {
                        int index = (int) (Math.random() * smileys.length);
                        String selectedSmiley = smileys[index];
                        grid.select(selectedSmiley);
                        grid.scrollToItem(selectedSmiley);
                    }

                    String potentialWinner = null;
                    for (Grid<String> grid : grids) {
                        if (potentialWinner == null) {
                            potentialWinner = grid.getSelectedItems().iterator().next();
                        } else {
                            String currentSelection = grid.getSelectedItems().iterator().next();
                            if (!potentialWinner.equals(currentSelection)) {
                                potentialWinner = null;
                                break;
                            }
                        }
                    }
                    if (potentialWinner != null) {
                        cashInCents += 2500;
                        System.out.println("We have a WINNER! Smiley: " + potentialWinner + ", New Cash: " + cashInCents + " cents");
                        updateCashDisplay();

                        // Trigger winner flash animation and skip next 8 rounds
                        addClassName("winner-flash");
                        skipRounds = 8;
                    } else {
                        System.out.println("No winner this round. Cash remaining: " + cashInCents + " cents");
                        updateCashDisplay();
                    }
                }
            });
        }, 1, 1, TimeUnit.SECONDS);

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

    private Grid<String> createSmileyGrid(List<String> smileys) {
        Grid<String> grid = new Grid<>();
        grid.setItems(smileys);
        grid.addColumn(smiley -> smiley).setHeader("Smiley");
        grid.setHeightFull();
        grid.setWidth("33%");
        grid.addClassName("large-rows");

        // Hide scrollbars after grid is attached and rendered
        grid.addAttachListener(event -> {
            grid.getElement().executeJs(
                "setTimeout(() => { " +
                "  const table = this.shadowRoot.getElementById('table'); " +
                "  console.log('Table element:', table); " +
                "  if (table) { " +
                "    table.style.overflow = 'hidden'; " +
                "    console.log('Overflow set to hidden'); " +
                "  } else { " +
                "    console.error('Table element not found in shadow DOM'); " +
                "  } " +
                "}, 200)"
            );
        });

        return grid;
    }

    private void updateCashDisplay() {
        double dollars = cashInCents / 100.0;
        cashDisplay.setText(String.format(Locale.US, "$%.2f", dollars));
    }
}

package com.example.myfabricmod.gui;

import com.example.myfabricmod.MyFabricModClient;
import com.example.myfabricmod.module.BooleanSetting;
import com.example.myfabricmod.module.Category;
import com.example.myfabricmod.module.ClientModule;
import com.example.myfabricmod.module.ColorSetting;
import com.example.myfabricmod.module.ModuleManager;
import com.example.myfabricmod.module.NumberSetting;
import com.example.myfabricmod.module.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The menu opened with Right Shift. Sidebar = categories, right side = the modules of the selected
 * category (or search results). Click a module's title row to switch it on/off, use - and + on number
 * settings, click boolean settings to flip them, scroll with the mouse wheel.
 */
public final class ClickGuiScreen extends Screen {
    private static final int PANEL_MAX_W = 560;
    private static final int PANEL_MAX_H = 340;
    private static final int SIDEBAR_W = 132;
    private static final int HEADER_H = 44;
    private static final int FOOTER_H = 24;

    private static final int CARD_HEAD_H = 26;
    private static final int SETTING_H = 18;
    private static final int CARD_GAP = 8;

    // Remembered while the game runs so the menu reopens on the same tab.
    private static Category lastCategory = Category.COMBAT;

    private Category selected = lastCategory;
    private String searchText = "";
    private double scroll;
    private double maxScroll;
    private EditBox searchBox;

    // Layout, recalculated from the window size by computeLayout().
    private int left;
    private int top;
    private int panelW;
    private int panelH;
    private int bottom;
    private int contentLeft;
    private int contentRight;
    private int searchTop;
    private int listTop;
    private int listBottom;

    /** A clickable rectangle in the module list, rebuilt every frame while drawing. */
    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
        boolean contains(double x, double y) {
            return x >= x1 && x < x2 && y >= y1 && y < y2;
        }
    }

    private final List<Hit> hits = new ArrayList<>();

    public ClickGuiScreen() {
        super(Component.literal("My Fabric Mod"));
    }

    private void computeLayout() {
        panelW = Math.min(PANEL_MAX_W, this.width - 32);
        panelH = Math.min(PANEL_MAX_H, this.height - 32);
        left = (this.width - panelW) / 2;
        top = (this.height - panelH) / 2;
        bottom = top + panelH;
        contentLeft = left + SIDEBAR_W + 16;
        contentRight = left + panelW - 16;
        searchTop = top + 57;
        listTop = searchTop + 27 + 10;
        listBottom = bottom - FOOTER_H - 6;
    }

    @Override
    protected void init() {
        computeLayout();
        searchBox = new EditBox(this.font, contentLeft + 10, searchTop + 6, contentRight - contentLeft - 20, 14,
            Component.literal("Search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(24);
        searchBox.setValue(searchText);
        searchBox.setResponder(text -> {
            searchText = text;
            scroll = 0;
        });
        this.addRenderableWidget(searchBox);
    }

    // ---------------------------------------------------------------- layout helpers

    /** Height of one category row in the sidebar. Shrinks on small windows so every category stays visible. */
    private int categoryRowHeight() {
        int available = (bottom - FOOTER_H) - (top + HEADER_H) - 12;
        return Math.max(16, Math.min(30, available / Category.values().length));
    }

    private int categoryTop(int index) {
        return top + 50 + index * categoryRowHeight();
    }

    private static int cardHeight(ClientModule module) {
        int settings = module.settings().size();
        return CARD_HEAD_H + (settings == 0 ? 0 : 3 + settings * SETTING_H + 3);
    }

    private List<ClientModule> visibleModules() {
        ModuleManager manager = MyFabricModClient.modules();
        String query = searchText.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return manager.inCategory(selected);
        }
        List<ClientModule> matches = new ArrayList<>();
        for (ClientModule module : manager.all()) {
            if (module.name().toLowerCase(Locale.ROOT).contains(query)) {
                matches.add(module);
            }
        }
        return matches;
    }

    private String fit(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String shortened = text;
        while (shortened.length() > 1 && this.font.width(shortened + "..") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "..";
    }

    private void selectCategory(Category category) {
        selected = category;
        lastCategory = category;
        scroll = 0;
        searchText = "";
        searchBox.setValue("");
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The game already draws the dimmed/blurred background before calling render(),
        // so it must not be drawn again here.
        computeLayout();
        hits.clear();

        // Main panel and border
        graphics.fill(left - 1, top - 1, left + panelW + 1, bottom + 1, 0xFF6D35E8);
        graphics.fill(left, top, left + panelW, bottom, 0xF20D0D16);
        graphics.fill(left, top, left + panelW, top + 3, 0xFF8B4DFF);

        // Header
        graphics.drawString(this.font, "MY FABRIC MOD", left + 16, top + 13, 0xFFF0E8FF, false);
        graphics.drawString(this.font, "CLIENT MENU", left + 16, top + 27, 0xFFAA91E8, false);
        graphics.drawString(this.font, "×", left + panelW - 18, top + 12, 0xFFD7C6FF, false);
        graphics.fill(left, top + HEADER_H - 1, left + panelW, top + HEADER_H, 0xFF30223F);

        // Sidebar
        graphics.fill(left, top + HEADER_H, left + SIDEBAR_W, bottom - FOOTER_H, 0xF7181722);
        graphics.fill(left + SIDEBAR_W, top + HEADER_H, left + SIDEBAR_W + 1, bottom - FOOTER_H, 0xFF30223F);

        Category[] categories = Category.values();
        int rowHeight = categoryRowHeight();
        for (int i = 0; i < categories.length; i++) {
            int rowTop = categoryTop(i);
            int rowBottom = rowTop + rowHeight - 3;
            boolean isSelected = searchText.trim().isEmpty() && categories[i] == selected;
            boolean hovered = mouseX >= left && mouseX < left + SIDEBAR_W && mouseY >= rowTop && mouseY < rowBottom;
            if (isSelected) {
                graphics.fill(left + 1, rowTop, left + SIDEBAR_W, rowBottom, 0xFF38205F);
                graphics.fill(left + 1, rowTop, left + 4, rowBottom, 0xFF9B5CFF);
            } else if (hovered) {
                graphics.fill(left + 1, rowTop, left + SIDEBAR_W, rowBottom, 0xFF211A2D);
            }
            int textY = rowTop + (rowHeight - 3 - 8) / 2;
            graphics.drawString(this.font, categories[i].displayName(), left + 16, textY,
                isSelected ? 0xFFE8D9FF : 0xFFB8A8D2, false);
        }

        // Search bar (the EditBox itself is drawn by super.render below)
        graphics.fill(contentLeft, searchTop, contentRight, searchTop + 27, 0xFF171522);
        graphics.fill(contentLeft, searchTop, contentRight, searchTop + 1, 0xFF49315F);
        graphics.fill(contentLeft, searchTop + 26, contentRight, searchTop + 27, 0xFF49315F);
        if (searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            graphics.drawString(this.font, "Search modules...", contentLeft + 10, searchTop + 9, 0xFF8E81AA, false);
        }

        drawModuleList(graphics, mouseX, mouseY);

        // Footer
        graphics.fill(left, bottom - FOOTER_H, left + panelW, bottom - FOOTER_H + 1, 0xFF30223F);
        graphics.drawCenteredString(this.font, "ESC TO CLOSE  •  CHANGE THE KEY IN OPTIONS > CONTROLS", left + panelW / 2, bottom - 15, 0xFFB7A0E8);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawModuleList(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ClientModule> modules = visibleModules();
        int centerX = (contentLeft + contentRight) / 2;
        int centerY = (listTop + listBottom) / 2;

        if (modules.isEmpty()) {
            boolean searching = !searchText.trim().isEmpty();
            graphics.drawCenteredString(this.font, searching ? "No modules match your search" : "No modules in " + selected.displayName() + " yet",
                centerX, centerY - 4, 0xFFE1D3FF);
            if (!searching) {
                graphics.drawCenteredString(this.font, "More features will appear here", centerX, centerY + 10, 0xFF8F849F);
            }
            maxScroll = 0;
            return;
        }

        int contentHeight = -CARD_GAP;
        for (ClientModule module : modules) {
            contentHeight += cardHeight(module) + CARD_GAP;
        }
        int viewHeight = listBottom - listTop;
        maxScroll = Math.max(0, contentHeight - viewHeight);
        scroll = Mth.clamp(scroll, 0.0, maxScroll);

        graphics.enableScissor(contentLeft - 1, listTop, contentRight + 1, listBottom);
        int y = listTop - (int) scroll;
        for (ClientModule module : modules) {
            drawCard(graphics, module, y, mouseX, mouseY);
            y += cardHeight(module) + CARD_GAP;
        }
        graphics.disableScissor();

        // Slim scroll bar when the list is longer than the view
        if (maxScroll > 0) {
            int barHeight = Math.max(16, (int) (viewHeight * (viewHeight / (double) contentHeight)));
            int barY = listTop + (int) ((viewHeight - barHeight) * (scroll / maxScroll));
            graphics.fill(contentRight + 4, barY, contentRight + 6, barY + barHeight, 0xFF6D35E8);
        }
    }

    private void drawCard(GuiGraphics graphics, ClientModule module, int y, int mouseX, int mouseY) {
        int x1 = contentLeft;
        int x2 = contentRight;
        int height = cardHeight(module);
        boolean on = module.isEnabled();
        boolean mouseInList = mouseY >= listTop && mouseY < listBottom;
        boolean hoverHead = mouseInList && mouseX >= x1 && mouseX < x2 && mouseY >= y && mouseY < y + CARD_HEAD_H;

        graphics.fill(x1, y, x2, y + height, 0xFF171522);
        graphics.fill(x1, y, x1 + 3, y + height, on ? 0xFF9B5CFF : 0xFF30223F);
        if (hoverHead) {
            graphics.fill(x1 + 3, y, x2, y + CARD_HEAD_H, 0xFF211A2D);
        }

        int pillW = 34;
        int pillH = 14;
        int pillX = x2 - pillW - 10;
        int textMaxWidth = pillX - (x1 + 12) - 8;
        graphics.drawString(this.font, fit(module.name(), textMaxWidth), x1 + 12, y + 5, 0xFFF0E8FF, false);
        graphics.drawString(this.font, fit(module.description(), textMaxWidth), x1 + 12, y + 15, 0xFF8E81AA, false);
        drawPill(graphics, pillX, y + (CARD_HEAD_H - pillH) / 2, pillW, pillH, on);
        hits.add(new Hit(x1, y, x2, y + CARD_HEAD_H, module::toggle));

        if (module.settings().isEmpty()) {
            return;
        }
        graphics.fill(x1 + 3, y + CARD_HEAD_H, x2, y + CARD_HEAD_H + 1, 0xFF211A2D);

        int rowTop = y + CARD_HEAD_H + 3;
        for (Setting setting : module.settings()) {
            graphics.drawString(this.font, fit(setting.name(), pillX - (x1 + 16) - 70), x1 + 16, rowTop + 5, 0xFFB8A8D2, false);

            if (setting instanceof NumberSetting number) {
                int buttonW = 16;
                int buttonH = 14;
                int valueW = 46;
                int plusX = x2 - 10 - buttonW;
                int valueX = plusX - valueW;
                int minusX = valueX - buttonW;
                int buttonY = rowTop + 2;

                graphics.fill(minusX, buttonY, minusX + buttonW, buttonY + buttonH, 0xFF2A2536);
                graphics.drawCenteredString(this.font, "-", minusX + buttonW / 2, buttonY + 3, 0xFFE8D9FF);
                graphics.drawCenteredString(this.font, number.display(), valueX + valueW / 2, buttonY + 3, 0xFFE1D3FF);
                graphics.fill(plusX, buttonY, plusX + buttonW, buttonY + buttonH, 0xFF2A2536);
                graphics.drawCenteredString(this.font, "+", plusX + buttonW / 2, buttonY + 3, 0xFFE8D9FF);

                hits.add(new Hit(minusX, rowTop, minusX + buttonW, rowTop + SETTING_H, () -> {
                    number.decrease();
                    MyFabricModClient.modules().save();
                }));
                hits.add(new Hit(plusX, rowTop, plusX + buttonW, rowTop + SETTING_H, () -> {
                    number.increase();
                    MyFabricModClient.modules().save();
                }));
            } else if (setting instanceof BooleanSetting flag) {
                drawPill(graphics, pillX, rowTop + 2, pillW, pillH, flag.get());
                hits.add(new Hit(x1 + 3, rowTop, x2, rowTop + SETTING_H, () -> {
                    flag.toggle();
                    MyFabricModClient.modules().save();
                }));
            } else if (setting instanceof ColorSetting colorSetting) {
                // Show a colored swatch
                int c = colorSetting.get() | 0xFF000000; // force opaque for display
                graphics.fill(pillX, rowTop + 2, pillX + pillW, rowTop + 2 + pillH, 0xFF222222);
                graphics.fill(pillX + 1, rowTop + 3, pillX + pillW - 1, rowTop + 1 + pillH, c);
            }
            rowTop += SETTING_H;
        }
    }

    private void drawPill(GuiGraphics graphics, int x, int y, int width, int height, boolean on) {
        graphics.fill(x, y, x + width, y + height, on ? 0xFF6D35E8 : 0xFF2A2536);
        graphics.drawCenteredString(this.font, on ? "ON" : "OFF", x + width / 2, y + (height - 8) / 2 + 1,
            on ? 0xFFFFFFFF : 0xFF8E81AA);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();

            // Close button (the x in the header)
            if (mouseX >= left + panelW - 26 && mouseX < left + panelW - 4 && mouseY >= top + 4 && mouseY < top + 24) {
                this.onClose();
                return true;
            }

            // Category sidebar
            if (mouseX >= left && mouseX < left + SIDEBAR_W) {
                Category[] categories = Category.values();
                int rowHeight = categoryRowHeight();
                for (int i = 0; i < categories.length; i++) {
                    int rowTop = categoryTop(i);
                    if (mouseY >= rowTop && mouseY < rowTop + rowHeight - 3) {
                        selectCategory(categories[i]);
                        return true;
                    }
                }
            }

            // Module list
            if (mouseY >= listTop && mouseY < listBottom) {
                for (Hit hit : hits) {
                    if (hit.contains(mouseX, mouseY)) {
                        hit.action().run();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - scrollY * 18.0, 0.0, maxScroll);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

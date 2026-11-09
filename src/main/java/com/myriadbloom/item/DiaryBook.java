package com.myriadbloom.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * 破旧不堪的日记：直接发放一本原版书（{@code minecraft:written_book}）。
 *
 * <p>客户端的开书逻辑只认原版书这个物品实例（{@code stack.is(Items.WRITTEN_BOOK)}），任何自定义物品
 * 都无法触发阅读界面，所以这里直接构造原版书并写入内容。页码是可翻译组件，随客户端语言变化。</p>
 */
public final class DiaryBook {
    private static final int PAGES = 6;

    private DiaryBook() {
    }

    /** 构造一本写好的日记。 */
    public static ItemStack create(String title) {
        List<Filterable<Component>> pages = java.util.stream.IntStream.rangeClosed(1, PAGES)
                .mapToObj(i -> Filterable.passThrough(
                        (Component) Component.translatable("book.myriadbloom.diary.page" + i)))
                .toList();
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(title), "???", 0, pages, true));
        return stack;
    }
}

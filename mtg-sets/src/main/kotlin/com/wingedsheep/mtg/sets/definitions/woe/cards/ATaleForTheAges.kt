package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * A Tale for the Ages
 * {1}{W}
 * Enchantment
 *
 * Enchanted creatures you control get +2/+2.
 *
 * A plain layer-7c group static (an anthem in the [GloriousAnthem][com.wingedsheep.mtg.sets.definitions.usg.cards.GloriousAnthem]
 * mould), the only novelty being the group itself: "enchanted" is
 * [StatePredicate.IsEnchanted][com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsEnchanted],
 * i.e. *has an Aura attached*, added alongside the pre-existing `IsEquipped`.
 *
 * The two adjectives in "enchanted creatures you control" bind to different objects and so are two
 * separate predicates: `youControl()` constrains the *creature's* controller, `enchanted()` only asks
 * that some Aura be attached to it. An opponent's Aura on your creature still turns the buff on
 * (CR 303.4 — an Aura enchants what it's attached to regardless of who controls the Aura), and your
 * Aura on their creature does not. Role tokens are Auras (CR 113.2c), so in practice this is the
 * Wilds of Eldraine Roles payoff — and note it is *not* `IsModified`: Equipment and counters don't
 * enchant anything.
 *
 * The buff is unconditional and self-independent (this enchantment is not itself an Aura), so no
 * `ConditionalStaticAbility` and no dependency wrinkle: it re-evaluates continuously as Auras come
 * and go, which is exactly what projected state gives us for free.
 */
val ATaleForTheAges = card("A Tale for the Ages") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Enchanted creatures you control get +2/+2."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().enchanted()),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Julie Dillon"
        flavorText = "With every retelling of Agatha's demise, Kellan got taller and Ruby got " +
            "stronger. But the core story stayed the same, and people remembered what happens to " +
            "those who prey on the weak."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca0c8d3b-ce30-4da5-a6a8-9bdcb3c757f9.jpg?1783915126"
    }
}

package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Destiny Spinner
 * {1}{G}
 * Enchantment Creature — Human
 * 2/3
 * Creature and enchantment spells you control can't be countered.
 * {3}{G}: Target land you control becomes an X/X Elemental creature with trample and haste until end
 * of turn, where X is the number of enchantments you control. It's still a land.
 *
 * - The uncounterable grant is Surrak Dragonclaw's [GrantCantBeCountered] over creature-or-enchantment
 *   spells you control.
 * - `BecomeCreature` evaluates X once as the ability resolves and stamps it as a fixed base P/T, so
 *   the land doesn't change size if your enchantment count changes later (ruling). No colors are
 *   set, so the land keeps its own (colorless for most lands).
 */
val DestinySpinner = card("Destiny Spinner") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Human"
    power = 2
    toughness = 3
    oracleText = "Creature and enchantment spells you control can't be countered.\n" +
        "{3}{G}: Target land you control becomes an X/X Elemental creature with trample and haste until end " +
        "of turn, where X is the number of enchantments you control. It's still a land."

    staticAbility {
        ability = GrantCantBeCountered(filter = GameObjectFilter.CreatureOrEnchantment.youControl())
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        val enchantments = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Enchantment).count()
        effect = Effects.BecomeCreature(
            target = land,
            power = enchantments,
            toughness = enchantments,
            keywords = setOf(Keyword.TRAMPLE, Keyword.HASTE),
            creatureTypes = setOf("Elemental"),
            duration = Duration.EndOfTurn,
        )
        description = "{3}{G}: Target land you control becomes an X/X Elemental creature with trample and haste " +
            "until end of turn, where X is the number of enchantments you control. It's still a land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba264166-948b-47d4-b302-64476acc1a55.jpg?1783931541"
        ruling("2020-01-24", "Destiny Spinner's first ability applies only while it's on the battlefield. While it's a spell, it can be countered.")
        ruling("2020-01-24", "The value of X is determined only as Destiny Spinner's activated ability resolves. Once that happens, the creature's base power and toughness won't change later in the turn even if the number of enchantments you control changes.")
        ruling("2020-01-24", "If you control no enchantments at the time Destiny Spinner's activated ability resolves, the land becomes a 0/0 creature and is put into its owner's graveyard unless another effect is raising its toughness.")
    }
}

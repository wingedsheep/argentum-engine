package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Wall Crawl
 * {3}{G}
 * Enchantment
 *
 * When this enchantment enters, create a 2/1 green Spider creature token with reach, then you gain
 * 1 life for each Spider you control.
 * Spiders you control get +1/+1 and can't be blocked by creatures with defender.
 *
 * The ETB is a single [Effects.Composite] so the life gain resolves *after* the token exists — the
 * newly-created Spider is counted (the token comes first, then the count). "1 life for each Spider
 * you control" is [DynamicAmount.AggregateBattlefield] with the default COUNT aggregation.
 *
 * The two anthem clauses are continuous static abilities keyed to "Spiders you control":
 *  - [ModifyStats] +1/+1 (Layer 7c) over the Spider group.
 *  - [CantBeBlockedBy] granting the evasion "can't be blocked by creatures with defender" to that
 *    same group (blockerFilter = creatures with defender).
 */
val WallCrawl = card("Wall Crawl") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a 2/1 green Spider creature token with reach, " +
        "then you gain 1 life for each Spider you control.\n" +
        "Spiders you control get +1/+1 and can't be blocked by creatures with defender."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Effects.CreateToken(
                power = 2,
                toughness = 1,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Spider"),
                keywords = setOf(Keyword.REACH),
                imageUri = "https://cards.scryfall.io/normal/front/4/a/4a40f6e1-3545-4503-af3e-f0acfb735e3a.jpg?1757379309"
            ),
            Effects.GainLife(
                DynamicAmount.AggregateBattlefield(
                    Player.You,
                    GameObjectFilter.Creature.withSubtype("Spider")
                )
            )
        )
    }

    // "Spiders you control get +1/+1" — Layer 7c anthem over the Spider group.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.SPIDER).youControl())
        )
    }

    // "... and can't be blocked by creatures with defender" over the same group.
    staticAbility {
        ability = CantBeBlockedBy(
            blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.SPIDER).youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Alexander Gering"
        flavorText = "With the proportionate strength and stickiness of a spider, Spider-Man can " +
            "scale skyscrapers with ease."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97a2a1ab-57ec-4210-9412-765ae4f02db0.jpg?1783905321"
    }
}

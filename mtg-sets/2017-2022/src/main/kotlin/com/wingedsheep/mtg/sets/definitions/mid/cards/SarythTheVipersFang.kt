package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Saryth, the Viper's Fang
 * {2}{G}{G}
 * Legendary Creature — Human Warlock
 * 3/4
 *
 * Other tapped creatures you control have deathtouch.
 * Other untapped creatures you control have hexproof.
 * {1}, {T}: Untap another target creature or land you control.
 *
 * The two grants are lords whose group filters carry a tapped/untapped state predicate, so they
 * are re-read every projection — a creature swaps deathtouch for hexproof as it taps and untaps.
 */
val SarythTheVipersFang = card("Saryth, the Viper's Fang") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Warlock"
    power = 3
    toughness = 4
    oracleText = "Other tapped creatures you control have deathtouch.\n" +
        "Other untapped creatures you control have hexproof.\n" +
        "{1}, {T}: Untap another target creature or land you control."

    staticAbility {
        ability = GrantKeyword(
            Keyword.DEATHTOUCH,
            GroupFilter(GameObjectFilter.Creature.tapped().youControl(), excludeSelf = true)
        )
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.HEXPROOF,
            GroupFilter(GameObjectFilter.Creature.untapped().youControl(), excludeSelf = true)
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val t = target(TargetFilter(GameObjectFilter.CreatureOrLand.youControl(), excludeSelf = true))
        effect = Effects.Untap(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "197"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc8b3a09-75e8-428c-91f7-8fac62fadc98.jpg?1783925573"
    }
}

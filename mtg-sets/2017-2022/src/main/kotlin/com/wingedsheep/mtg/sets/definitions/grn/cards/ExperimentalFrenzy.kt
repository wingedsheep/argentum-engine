package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.PlayersCantPlayLands
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Experimental Frenzy
 * {3}{R}
 * Enchantment
 * You may look at the top card of your library any time.
 * You may play lands and cast spells from the top of your library.
 * You can't play lands or cast spells from your hand.
 * {3}{R}: Destroy this enchantment.
 *
 * The top card is looked at privately ([LookAtTopOfLibrary]), not revealed, so the play permission
 * is the non-revealing [PlayLandsAndCastFilteredFromTopOfLibrary] over every spell (One with the
 * Multiverse's pair). The hand lock is two zone-scoped prohibitions: [PlayersCantCastSpells] with
 * `fromZones = HAND`, and [PlayersCantPlayLands] filtered to cards currently in hand — so the land
 * on top of the library stays playable. Other uses of hand cards (cycling, discarding) are untouched,
 * per the ruling.
 */
val ExperimentalFrenzy = card("Experimental Frenzy") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may play lands and cast spells from the top of your library.\n" +
        "You can't play lands or cast spells from your hand.\n" +
        "{3}{R}: Destroy this enchantment."

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = GameObjectFilter.Any)
    }

    staticAbility {
        ability = PlayersCantPlayLands(
            affected = Player.You,
            landFilter = GameObjectFilter.Any.currentlyIn(Zone.HAND),
        )
    }

    staticAbility {
        ability = PlayersCantCastSpells(Player.You, fromZones = setOf(Zone.HAND))
    }

    activatedAbility {
        cost = Costs.Mana("{3}{R}")
        effect = Effects.Destroy(EffectTarget.Self)
        description = "{3}{R}: Destroy this enchantment."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b8f32e2-5dc8-4f1b-8a69-d3ae06378ed8.jpg?1783934164"
        ruling(
            "2018-10-05",
            "You can play a land card on top of your library only if you have available land plays remaining.",
        )
        ruling(
            "2018-10-05",
            "You'll still pay all costs for a spell you cast from your library, including additional costs. " +
                "You may also pay alternative costs.",
        )
        ruling(
            "2018-10-05",
            "You can't cast spells or play lands from your hand, but you may take other actions with those " +
                "cards (such as discarding them to activate their cycling or bloodrush abilities).",
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeSacrificed
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantPayLifeForCosts
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Yasharn, Implacable Earth — Zendikar Rising #240
 * {2}{G}{W} · Legendary Creature — Elemental Boar · 4/4
 *
 * When Yasharn enters, search your library for a basic Forest card and a basic Plains card, reveal
 * those cards, put them into your hand, then shuffle.
 * Players can't pay life or sacrifice nonland permanents to cast spells or activate abilities.
 *
 * The enters trigger is **one** search (one "searches their library" event), not two: the pool is
 * every basic Forest and basic Plains card, and [SelectionRestriction.OnePerBasicLandType] caps the
 * pick at one card of each type. Up to two, because either half may be left unfound (ruling).
 *
 * The last ability is two cost prohibitions scoped to casting spells and activating abilities (mana
 * abilities included), the same pair Angel of Jubilation uses: [PlayersCantPayLifeForCosts] and
 * [CantBeSacrificed] with [CantBeSacrificed.Scope.CASTING_AND_ACTIVATION_COSTS] over every nonland
 * permanent. A resolving spell or ability may still make a player pay life or sacrifice.
 */
val YasharnImplacableEarth = card("Yasharn, Implacable Earth") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elemental Boar"
    power = 4
    toughness = 4
    oracleText = "When Yasharn enters, search your library for a basic Forest card and a basic Plains card, " +
        "reveal those cards, put them into your hand, then shuffle.\n" +
        "Players can't pay life or sacrifice nonland permanents to cast spells or activate abilities."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    filter = GameObjectFilter.BasicLand.withSubtype(Subtype.FOREST) or
                        GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS)
                ),
                search = true
            )
            val found = chooseUpTo(
                2,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerBasicLandType),
                prompt = "Search your library for a basic Forest card and a basic Plains card to put into your hand",
                selectedLabel = "Put into your hand"
            )
            toHand(found, revealed = true)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    staticAbility {
        ability = PlayersCantPayLifeForCosts()
    }

    staticAbility {
        ability = CantBeSacrificed(GroupFilter.AllNonlandPermanents, CantBeSacrificed.Scope.CASTING_AND_ACTIVATION_COSTS)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "G-host Lee"
        flavorText = "In its hoofprints, barren dust becomes fertile ground."
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12878fbd-dbe5-44bc-9f34-38fd374ec10a.jpg?1783929317"
        ruling("2020-09-25", "Yasharn's first ability can find just one basic Forest or one basic Plains card if you don't have both in your library or if you don't wish to find both.")
        ruling("2020-09-25", "If a spell or activated ability has a cost that requires a player to pay life (such as the activated ability granted by Lithoform Blight) or sacrifice a permanent (such as Kazuul's Fury or Spare Supplies), that spell or ability can't be cast or activated.")
        ruling("2020-09-25", "Other things may still cause players to pay life or sacrifice creatures, such as a resolving spell or ability.")
    }
}

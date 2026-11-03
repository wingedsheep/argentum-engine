package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Genesis Device
 * {2}{G}
 * Legendary Artifact
 * When Genesis Device enters, search your library for a basic land card or Planet card, put it onto
 * the battlefield tapped, then shuffle.
 * {3}{G}{G}, {T}, Sacrifice Genesis Device: Sacrifice any number of lands you control, then search
 * your library for up to that many land cards, put them onto the battlefield tapped, then shuffle.
 * Activate only as a sorcery.
 *
 * The enters trigger is Final Frontier's search. The activated ability is Scapeshift's resolution:
 * the lands are sacrificed on resolution (not as a cost), and "up to that many" reads
 * `permanentsSacrificedThisWay`. The Device itself, sacrificed as a cost, is not counted — only
 * `CostAtom.Sacrifice` choices are snapshotted into the ability's sacrificed list.
 */
val GenesisDevice = card("Genesis Device") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Artifact"
    oracleText = "When Genesis Device enters, search your library for a basic land card or Planet " +
        "card, put it onto the battlefield tapped, then shuffle.\n" +
        "{3}{G}{G}, {T}, Sacrifice Genesis Device: Sacrifice any number of lands you control, then " +
        "search your library for up to that many land cards, put them onto the battlefield tapped, " +
        "then shuffle. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype("Planet"),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{G}{G}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.SacrificeAnyNumber(GameObjectFilter.Land) then
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Land,
                count = DynamicAmounts.permanentsSacrificedThisWay(),
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        timing = TimingRule.SorcerySpeed
        description = "{3}{G}{G}, {T}, Sacrifice Genesis Device: Sacrifice any number of lands you " +
            "control, then search your library for up to that many land cards, put them onto the " +
            "battlefield tapped, then shuffle. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Cliff Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a5b94ba-ac1f-478e-981f-7dfc31ad02f9.jpg?1793188016"
    }
}

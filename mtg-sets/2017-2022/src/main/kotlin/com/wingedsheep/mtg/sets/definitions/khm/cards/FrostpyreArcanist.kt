package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Frostpyre Arcanist
 * {4}{U}
 * Creature — Giant Wizard
 * 2/5
 * This spell costs {1} less to cast if you control a Giant or a Wizard.
 * When this creature enters, search your library for an instant or sorcery card with the same name
 * as a card in your graveyard, reveal it, put it into your hand, then shuffle.
 *
 * The search filter is `SharesNameWithYourCard` pointed at your graveyard: only instants and
 * sorceries sharing a name with some card there are selectable, and with none the search finds
 * nothing (the library is still shuffled).
 */
val FrostpyreArcanist = card("Frostpyre Arcanist") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Giant Wizard"
    power = 2
    toughness = 5
    oracleText = "This spell costs {1} less to cast if you control a Giant or a Wizard.\n" +
        "When this creature enters, search your library for an instant or sorcery card with the same name " +
        "as a card in your graveyard, reveal it, put it into your hand, then shuffle."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(1),
            gating = CostGating.OnlyIf(
                Conditions.YouControl(
                    GameObjectFilter.Permanent.withAnyOfSubtypes(listOf(Subtype.GIANT, Subtype.WIZARD))
                )
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.InstantOrSorcery.sharingNameWithCardInYour(Zone.GRAVEYARD),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Sam Rowan"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/276f8abd-4d39-4b50-876f-9c6713bc4ab5.jpg?1783928263"
    }
}

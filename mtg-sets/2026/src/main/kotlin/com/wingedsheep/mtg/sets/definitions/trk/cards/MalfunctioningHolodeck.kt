package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Malfunctioning Holodeck
 * {2}{W}
 * Artifact
 *
 * Ward {2} (Whenever this artifact becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)
 * When this artifact enters, exile target nonland permanent an opponent controls until this artifact leaves the battlefield.
 */
val MalfunctioningHolodeck = card("Malfunctioning Holodeck") {
    manaCost = "{2}{W}"
    typeLine = "Artifact"
    colorIdentity = "W"
    oracleText = "Ward {2} (Whenever this artifact becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)\nWhen this artifact enters, exile target nonland permanent an opponent controls until this artifact leaves the battlefield."
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))
    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.MoveUntilSourceLeaves(permanent, Zone.EXILE)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Pablo Mendoza"
        flavorText = "\"The holodeck safeties are still engaged, right?\"\n—Ensign Kei"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0faeffee-bb07-4b9d-8cdd-e9731bbd2396.jpg?1785981349"
    }
}


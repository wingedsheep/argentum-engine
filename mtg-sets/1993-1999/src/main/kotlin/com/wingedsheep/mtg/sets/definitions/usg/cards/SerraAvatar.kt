package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serra Avatar
 * {4}{W}{W}{W}
 * Creature — Avatar
 * &#42;/&#42;
 * Serra Avatar's power and toughness are each equal to your life total.
 * When Serra Avatar is put into a graveyard from anywhere, shuffle it into its owner's library.
 *
 * The P/T is a characteristic-defining ability read from the controller's current life total.
 * The graveyard trigger is the Purity shape: it fires from the graveyard, so its controller is the
 * card's owner, and the shuffle is unconditional — per the 2022-12-08 ruling, if the Avatar has
 * already left the graveyard the move does nothing but the owner still shuffles their library.
 */
val SerraAvatar = card("Serra Avatar") {
    manaCost = "{4}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Avatar"
    oracleText = "Serra Avatar's power and toughness are each equal to your life total.\n" +
        "When Serra Avatar is put into a graveyard from anywhere, shuffle it into its owner's library."

    dynamicStats(DynamicAmounts.yourLifeTotal())

    triggeredAbility {
        triggerZone = Zone.GRAVEYARD
        trigger = Triggers.self.changesZone(to = Zone.GRAVEYARD)
        effect = Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.GRAVEYARD) then
            Effects.ShuffleLibrary()
        description = "When Serra Avatar is put into a graveyard from anywhere, shuffle it into its owner's library."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Dermot Power"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/288b0976-78e8-4fbe-8607-2e55d8761d3e.jpg?1783946368"
        ruling("2022-12-08", "The ability that defines Serra Avatar's power and toughness works in all zones, not only the battlefield.")
        ruling("2022-12-08", "If Serra Avatar is no longer in your graveyard when its triggered ability resolves, you still shuffle your library.")
    }
}

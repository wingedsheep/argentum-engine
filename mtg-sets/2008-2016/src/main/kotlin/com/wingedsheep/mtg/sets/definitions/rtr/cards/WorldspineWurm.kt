package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Worldspine Wurm
 * {8}{G}{G}{G}
 * Creature — Wurm
 * 15/15
 * Trample
 * When this creature dies, create three 5/5 green Wurm creature tokens with trample.
 * When Worldspine Wurm is put into a graveyard from anywhere, shuffle it into its owner's library.
 *
 * Modeling notes:
 *  - The shuffle is a triggered ability (not a replacement), fired from the graveyard on any zone
 *    change into it, as with Lorwyn's Purity/Vigor and Kozilek. If the Wurm leaves the graveyard
 *    before it resolves, the move finds nothing but the owner's library is still shuffled.
 *  - Dying fires both triggers; the tokens are made regardless of where the card has gone.
 */
val WorldspineWurm = card("Worldspine Wurm") {
    manaCost = "{8}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 15
    toughness = 15
    oracleText = "Trample\n" +
        "When this creature dies, create three 5/5 green Wurm creature tokens with trample.\n" +
        "When Worldspine Wurm is put into a graveyard from anywhere, shuffle it into its owner's library."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wurm"),
            keywords = setOf(Keyword.TRAMPLE),
            count = 3,
            imageUri = "https://cards.scryfall.io/normal/front/3/3/33ee3f6c-5df6-4271-b2f9-86b9afffab7b.jpg?1783940381"
        )
        description = "When this creature dies, create three 5/5 green Wurm creature tokens with trample."
    }

    triggeredAbility {
        triggerZone = Zone.GRAVEYARD
        trigger = Triggers.self.changesZone(to = Zone.GRAVEYARD)
        effect = Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.GRAVEYARD) then
            Effects.ShuffleLibrary(EffectTarget.PlayerRef(Player.OwnerOfSource))
        description = "When Worldspine Wurm is put into a graveyard from anywhere, shuffle it into its owner's library."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "140"
        artist = "Richard Wright"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/543d55cb-3a6b-4620-af25-10ae74ed32c4.jpg?1783940344"
        ruling("2012-10-01", "Worldspine Wurm's last ability is a triggered ability, not a replacement ability. Players can respond to this ability, for example, by trying to exile Worldspine Wurm from the graveyard before it's shuffled into a library.")
    }
}

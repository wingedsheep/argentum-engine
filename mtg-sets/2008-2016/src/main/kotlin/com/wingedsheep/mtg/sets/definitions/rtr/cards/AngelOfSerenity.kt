package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Angel of Serenity
 * {4}{W}{W}{W}
 * Creature — Angel
 * 5/6
 * Flying
 * When this creature enters, you may exile up to three other target creatures from the
 * battlefield and/or creature cards from graveyards.
 * When this creature leaves the battlefield, return the exiled cards to their owners' hands.
 *
 * [com.wingedsheep.mtg.sets.definitions.mkm.cards.AureliasVindicator]'s shape with a fixed
 * count of three: one cross-zone target requirement (`OtherCreature` or `CreatureInGraveyard`),
 * `optional` for "up to", and a gather-and-exile pipeline linked to the source rather than
 * [Effects.ExileUntilLeaves] — per the ruling, if the Angel has already left when the ETB
 * resolves, the targets are still exiled, indefinitely. The "you may" is chosen on resolution
 * ([Effects.May]); the targets are chosen when the trigger goes on the stack.
 */
val AngelOfSerenity = card("Angel of Serenity") {
    manaCost = "{4}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 5
    toughness = 6
    oracleText = "Flying\n" +
        "When this creature enters, you may exile up to three other target creatures from the battlefield " +
        "and/or creature cards from graveyards.\n" +
        "When this creature leaves the battlefield, return the exiled cards to their owners' hands."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter.OtherCreature.or(TargetFilter.CreatureInGraveyard),
            count = 3,
            optional = true,
        )
        effect = Effects.May(
            Effects.Pipeline {
                val exiled = gather(CardSource.ChosenTargets)
                exile(exiled, linkToSource = true)
            }
        )
        description = "When this creature enters, you may exile up to three other target creatures from " +
            "the battlefield and/or creature cards from graveyards."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileToHand()
        description = "When this creature leaves the battlefield, return the exiled cards to their owners' hands."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f10d82f7-7759-457e-a9bb-f9a5bd968f82.jpg?1783940378"

        ruling(
            "2013-01-24",
            "You choose up to three total targets from among other creatures on the battlefield and creature " +
                "cards in graveyards. You may choose zero targets."
        )
        ruling(
            "2013-01-24",
            "You choose the targets as part of putting the \"enters\" trigger on the stack. Because that " +
                "ability includes \"you may,\" you choose whether to exile the targets when the ability resolves."
        )
        ruling(
            "2013-01-24",
            "If a creature targeted by the \"enters\" ability dies before that ability resolves, it will become " +
                "an illegal target even though it may be a creature card in a graveyard when the ability " +
                "resolves. It won't be exiled."
        )
        ruling(
            "2012-10-01",
            "If Angel of Serenity leaves the battlefield before its \"enters\" ability has resolved, its " +
                "leaves-the-battlefield ability will trigger and do nothing. Then the \"enters\" ability will " +
                "resolve and exile the targeted creatures and/or creature cards indefinitely."
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lead Pipe — Murders at Karlov Manor #90
 * {B} · Artifact — Clue Equipment
 *
 * Equipped creature gets +2/+0.
 * Whenever equipped creature dies, each opponent loses 1 life.
 * {2}, Sacrifice this Equipment: Draw a card.
 * Equip {2}
 *
 * One of the set's "murder weapon" Equipment — an Equipment that is also a Clue, so it carries the
 * standard Clue sacrifice-to-draw *and* the Clue subtype, which matters for the set's "sacrifice a
 * Clue" payoffs (Scryfall's ruling: "If an effect refers to a Clue, it means any Clue artifact,
 * not just a Clue artifact token"). The subtype is on the type line, so nothing extra is needed
 * for those payoffs to see it.
 *
 * The death trigger is [TriggerBinding.ATTACHED] over a battlefield→graveyard zone change, which
 * is the engine's "equipped creature dies" idiom. The Equipment stays on the battlefield when its
 * host dies (it just becomes unattached, CR 704.5m), so it can be re-equipped or cashed in for the
 * card afterwards — the two halves of the card don't compete.
 */
val LeadPipe = card("Lead Pipe") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Clue Equipment"
    oracleText = "Equipped creature gets +2/+0.\n" +
        "Whenever equipped creature dies, each opponent loses 1 life.\n" +
        "{2}, Sacrifice this Equipment: Draw a card.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(+2, +0, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(to = Zone.GRAVEYARD, binding = TriggerBinding.ATTACHED)
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever equipped creature dies, each opponent loses 1 life."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87f69249-c6e4-40c1-9870-b9c45ce24c39.jpg?1783912896"

        ruling(
            "2016-04-08",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact " +
                "token."
        )
        ruling(
            "2016-04-08",
            "You can't sacrifice a Clue to pay multiple costs."
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion

/**
 * Knowledge Vault
 * {4}
 * Artifact
 *
 * {2}, {T}: Exile the top card of your library face down.
 * {0}: Sacrifice this artifact. If you do, discard your hand, then put all cards exiled with this
 * artifact into their owner's hand.
 * When this artifact leaves the battlefield, put all cards exiled with it into their owner's
 * graveyard.
 *
 * Modelling notes:
 * - The exile is face down with no turn-up procedure ([FaceDownMode.HIDDEN]) and linked to this
 *   Vault (CR 607 linked abilities) — ruling: "You don't get to look at the cards exiled with
 *   Knowledge Vault."
 * - The sacrifice happens on resolution, not as a cost (ruling). [Effects.IfYouDo] gates the rest on
 *   the sacrifice actually moving the Vault: if it already left the battlefield (or changed
 *   controllers, so it isn't on the activator's battlefield) nothing happens.
 * - The pile is read after the Vault has left the battlefield; the linked-exile lookup follows the
 *   resolving ability's source visit, so it still finds the cards. Hand/graveyard moves route to
 *   each card's owner.
 * - The leaves trigger fires after the {0} ability finishes; by then those cards are in hand and
 *   no longer "exiled with" the Vault, so it finds nothing. When the Vault leaves any other way, it
 *   bins the pile.
 */
val KnowledgeVault = card("Knowledge Vault") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Exile the top card of your library face down.\n" +
        "{0}: Sacrifice this artifact. If you do, discard your hand, then put all cards exiled " +
        "with this artifact into their owner's hand.\n" +
        "When this artifact leaves the battlefield, put all cards exiled with it into their " +
        "owner's graveyard."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.Pipeline {
            val vaultTop = gather(CardSource.TopOfLibrary(1))
            exile(vaultTop, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
        }
        description = "{2}, {T}: Exile the top card of your library face down."
    }

    activatedAbility {
        cost = Costs.Free
        effect = Effects.IfYouDo(
            action = SacrificeSelfEffect,
            then = Patterns.Hand.discardHand() then Effects.Pipeline {
                val vaultContents = gather(CardSource.FromLinkedExile())
                toHand(vaultContents)
            },
            successCriterion = SuccessCriterion.PermanentsSacrificed,
            descriptionOverride = "Sacrifice this artifact. If you do, discard your hand, then " +
                "put all cards exiled with this artifact into their owner's hand.",
        )
        description = "{0}: Sacrifice this artifact. If you do, discard your hand, then put all " +
            "cards exiled with this artifact into their owner's hand."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.Pipeline {
            val vaultLeftovers = gather(CardSource.FromLinkedExile())
            toGraveyard(vaultLeftovers)
        }
        description = "When this artifact leaves the battlefield, put all cards exiled with it " +
            "into their owner's graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "281"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65947312-75c2-4baa-805c-238a154156ef.jpg?1783948028"
        ruling("2009-10-01", "You don't get to look at the cards exiled with Knowledge Vault.")
        ruling("2009-10-01", "If you activate Knowledge Vault's second ability, all that happens at that time is that you pay a cost of {0}. As the ability resolves, you must sacrifice Knowledge Vault. This is mandatory. If you are able to do so, you discard your hand and put all cards exiled with Knowledge Vault into their owners' hands. If you aren't able to do so because Knowledge Vault left the battlefield or changed controllers before the ability resolves, nothing happens.")
        ruling("2008-10-01", "You can activate the second ability any time you have priority, including in response to itself. However, you will only be able to sacrifice the Vault as the first such ability resolves.")
    }
}

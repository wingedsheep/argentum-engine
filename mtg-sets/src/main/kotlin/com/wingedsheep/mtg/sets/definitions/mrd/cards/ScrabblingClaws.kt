package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Scrabbling Claws — Mirrodin #237
 * {1} · Artifact
 *
 * {T}: Target player exiles a card from their graveyard.
 * {1}, Sacrifice this artifact: Exile target card from a graveyard. Draw a card.
 *
 * The two abilities differ in *who chooses*, and that difference is the whole card:
 *
 *  - The tap ability targets a **player**, and that player picks which of their own cards to
 *    exile. It is the Chimney Imp shape — Gather → Select → Move over the target player's
 *    graveyard with [Chooser.TargetPlayer]. An empty graveyard gathers nothing, so the selection
 *    is skipped and the ability resolves as a no-op; a player is a legal target regardless.
 *  - The sacrifice ability targets a **card in any graveyard**, so the Claws' controller picks,
 *    and it is a plain [TargetObject] over [TargetFilter.CardInGraveyard].
 *
 * The sacrifice is part of the cost, so the Claws are already in the graveyard when the ability
 * resolves and can themselves be the sacrificed-then-exiled card only via the *other* copy of the
 * ability — nothing here reads the source's battlefield state. If the targeted card leaves the
 * graveyard in response the ability is countered on resolution and no card is drawn.
 */
val ScrabblingClaws = card("Scrabbling Claws") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Target player exiles a card from their graveyard.\n" +
        "{1}, Sacrifice this artifact: Exile target card from a graveyard. Draw a card."

    activatedAbility {
        cost = Costs.Tap
        target = TargetPlayer()
        effect = Effects.Pipeline {
            val graveyard = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                name = "clawsGraveyard"
            )
            val chosen = chooseExactly(
                1,
                from = graveyard,
                chooser = Chooser.TargetPlayer,
                prompt = "Exile a card from your graveyard",
                name = "clawsExiled"
            )
            exile(chosen, owner = Player.ContextPlayer(0))
        }
        description = "{T}: Target player exiles a card from their graveyard."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        val exiled = target("target card in a graveyard", TargetObject(filter = TargetFilter.CardInGraveyard))
        effect = Effects.Composite(
            Effects.Move(exiled, Zone.EXILE),
            Effects.DrawCards(1)
        )
        description = "{1}, Sacrifice this artifact: Exile target card from a graveyard. Draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Thomas M. Baxa"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/415027f8-ccef-4b38-ace2-db4e94f066fe.jpg?1783944505"
    }
}

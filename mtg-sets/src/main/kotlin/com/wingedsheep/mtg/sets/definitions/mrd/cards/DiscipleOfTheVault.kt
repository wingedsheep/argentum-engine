package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Disciple of the Vault
 * {B}
 * Creature — Human Cleric
 * 1/1
 * Whenever an artifact is put into a graveyard from the battlefield, you may have target
 * opponent lose 1 life.
 *
 * Any artifact, any controller — hence `TriggerBinding.ANY` with a bare
 * [GameObjectFilter.Artifact] rather than a `youControl()` scope or a SELF binding. Artifact
 * *creatures* and artifact tokens count: both are artifacts put into a graveyard from the
 * battlefield.
 *
 * The "you may" is an explicit [MayEffect] rather than `optional = true` on the ability. With a
 * *player* target the bare `optional` flag is silently lost: `TriggerProcessor` auto-selects the
 * sole legal opponent and puts the trigger straight on the stack, skipping the target-selection
 * decision that would otherwise carry the decline. A `MayEffect` owns its own consent gate, so
 * the trigger routes through the may-then-target path and always asks.
 */
val DiscipleOfTheVault = card("Disciple of the Vault") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    oracleText = "Whenever an artifact is put into a graveyard from the battlefield, you may " +
        "have target opponent lose 1 life."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Artifact,
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        val opponent = target("target opponent", Targets.Opponent)
        effect = MayEffect(
            Effects.LoseLife(1, opponent),
            descriptionOverride = "You may have target opponent lose 1 life"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Matt Thompson"
        flavorText = "He stands in the shadow of his lord, Geth, drinking in the dark " +
            "energies of the Vault."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/644359dc-3c4c-4291-876d-7390dc466877.jpg?1783944548"
        ruling(
            "2020-08-07",
            "If an artifact is put into a graveyard at the same time as Disciple of the Vault, " +
                "its ability triggers for that artifact."
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Iron Man, Master of Machines — Marvel Super Heroes #216
 * {2}{U}{R} · Legendary Artifact Creature — Human Hero · 1/4
 *
 * Flying, vigilance
 * Iron Man gets +1/+0 for each other artifact you control.
 * Whenever Iron Man attacks, if an artifact entered the battlefield under your control this turn,
 * draw a card.
 *
 * Modeling notes:
 *  - The self-pump is Persistent Marshstalker's shape: a [GrantDynamicStatsEffect] over
 *    `GroupFilter.source()` whose power bonus counts artifacts you control with `excludeSelf = true`
 *    ("each *other* artifact"). Iron Man is himself an artifact, so the exclusion is load-bearing.
 *    Keeping it a Layer 7c continuous effect (rather than a one-shot) lets the projector re-evaluate
 *    it as artifacts come and go mid-combat.
 *  - "Whenever Iron Man attacks, if …" is an intervening-if clause (CR 603.4): checked both when the
 *    trigger would go on the stack and again on resolution, which is exactly what `triggerCondition`
 *    models — *not* a `ConditionalEffect` inside the body.
 *  - The condition reads the ETB-by-type *event* tracker
 *    ([Conditions.ArtifactEnteredBattlefieldThisTurn], Mechan Shieldmate's precedent), not the
 *    current battlefield population: an artifact that entered and then left (or stopped being an
 *    artifact) still satisfies "an artifact entered the battlefield under your control this turn".
 */
val IronManMasterOfMachines = card("Iron Man, Master of Machines") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Artifact Creature — Human Hero"
    power = 1
    toughness = 4
    oracleText = "Flying, vigilance\n" +
        "Iron Man gets +1/+0 for each other artifact you control.\n" +
        "Whenever Iron Man attacks, if an artifact entered the battlefield under your control this " +
        "turn, draw a card."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    // Iron Man gets +1/+0 for each other artifact you control.
    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = GameObjectFilter.Artifact,
                excludeSelf = true,
            ),
            toughnessBonus = DynamicAmount.Fixed(0),
        )
    }

    // Whenever Iron Man attacks, if an artifact entered the battlefield under your control this
    // turn, draw a card.
    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.ArtifactEnteredBattlefieldThisTurn
        effect = Effects.DrawCards(1)
        description = "Whenever Iron Man attacks, if an artifact entered the battlefield under " +
            "your control this turn, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "John Tyler Christopher"
        flavorText = "\"The future is what we make it.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f84ab0a-bf6e-4f28-9da8-998512a224ed.jpg?1783902901"
    }
}

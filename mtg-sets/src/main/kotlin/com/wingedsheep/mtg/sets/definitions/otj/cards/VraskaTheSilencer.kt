package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.BecomeArtifactEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vraska, the Silencer {1}{B}{G}
 * Legendary Creature — Gorgon Assassin
 * 3/3
 *
 * Deathtouch
 * Whenever a nontoken creature an opponent controls dies, you may pay {1}. If you do, return
 * that card to the battlefield tapped under your control. It's a Treasure artifact with
 * "{T}, Sacrifice this artifact: Add one mana of any color," and it loses all other card types.
 *
 * The transform is modeled with [BecomeArtifactEffect] (Duration.Permanent), which stacks
 * continuous floating effects on the returned entity: SetCardTypes(ARTIFACT) + SetAllSubtypes
 * (Treasure) (Layer 4), colorless (Layer 5), RemoveAllAbilities (Layer 6), plus the granted
 * sac-for-mana ability (durable, survives the ability wipe). "Nontoken" + "an opponent controls"
 * restrict the dies trigger; [EffectTarget.TriggeringEntity] is the dying card, now in the
 * graveyard, which [Effects.Move] returns to the battlefield tapped under your control.
 */
val VraskaTheSilencer = card("Vraska, the Silencer") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Gorgon Assassin"
    oracleText = "Deathtouch\n" +
        "Whenever a nontoken creature an opponent controls dies, you may pay {1}. If you do, " +
        "return that card to the battlefield tapped under your control. It's a Treasure artifact " +
        "with \"{T}, Sacrifice this artifact: Add one mana of any color,\" and it loses all other " +
        "card types."
    power = 3
    toughness = 3
    keywords(Keyword.DEATHTOUCH)

    // The Treasure mana ability granted to the returned card (mirrors the predefined Treasure token).
    val treasureManaAbility = ActivatedAbility(
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf),
        effect = Effects.AddAnyColorMana(1),
        isManaAbility = true,
        descriptionOverride = "{T}, Sacrifice this artifact: Add one mana of any color."
    )

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.nontoken().opponentControls(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = Effects.Composite(
                // Return that card to the battlefield tapped under your control.
                Effects.Move(
                    target = EffectTarget.TriggeringEntity,
                    destination = Zone.BATTLEFIELD,
                    placement = ZonePlacement.Tapped,
                    controllerOverride = EffectTarget.Controller
                ),
                // It's a colorless Treasure artifact with the mana ability, losing all other types.
                BecomeArtifactEffect(
                    target = EffectTarget.TriggeringEntity,
                    cardTypes = setOf("ARTIFACT"),
                    subtypes = setOf("Treasure"),
                    colors = emptySet(),
                    loseAllAbilities = true,
                    grantedAbility = treasureManaAbility,
                    duration = Duration.Permanent
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "237"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b042abf2-c40b-4235-a4fa-2e4901c375c3.jpg?1712356238"
    }
}

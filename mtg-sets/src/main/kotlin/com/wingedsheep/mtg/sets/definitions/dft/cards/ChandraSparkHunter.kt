package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Chandra, Spark Hunter — Aetherdrift #116
 * {3}{R} · Legendary Planeswalker — Chandra · Starting loyalty 4
 *
 * At the beginning of combat on your turn, choose up to one target Vehicle you control. Until end
 * of turn, it becomes an artifact creature and gains haste.
 * +2: You may sacrifice an artifact or discard a card. If you do, draw a card.
 * 0: Create a 3/2 colorless Vehicle artifact token with crew 1.
 * −7: You get an emblem with "Whenever an artifact you control enters, this emblem deals 3 damage
 *     to any target."
 *
 * Modeling notes:
 *
 *  - **The combat trigger** is a loyalty-independent triggered ability, not a loyalty ability, so
 *    it lives in a plain `triggeredAbility { }` on [Triggers.BeginCombat] ("at the beginning of
 *    combat on your turn"). "Up to one target" is `optional = true`; with no target chosen the
 *    ability still resolves and simply does nothing. The Vehicle filter is a bare subtype filter
 *    over `GameObjectFilter.Any`, not `Creature` — an uncrewed Vehicle is a *noncreature* artifact,
 *    which is exactly the case this ability exists to fix.
 *  - **The animation** is [Effects.AddCardType] "Creature" for the turn, the same Layer-4 grant
 *    crew uses (Kolodin, Triumph Caster): a Vehicle is already an artifact (CR 301.7) and its
 *    printed P/T and keywords apply the moment it is a creature, so the type grant is the whole
 *    animation. Haste is a separate Layer-6 grant, and it matters — the animated Vehicle can attack
 *    the turn Chandra points at it even though it has been a noncreature all along.
 *  - **The +2** is a "you may" wrapped around a two-way choice, with the draw folded into each
 *    branch. That is equivalent to the printed "If you do, draw a card" and avoids an
 *    action-outcome gate: [com.wingedsheep.sdk.scripting.effects.SuccessCriterion] `Auto` can only
 *    infer success from a terminal zone move, which a [Effects.ChooseAction] isn't. The
 *    [FeasibilityCheck]s hide a branch the player can't actually take, and when neither is
 *    available the choice resolves to nothing — so no draw, matching the printed gate.
 *  - **The 0** makes the set's standard 3/2 crew-1 Vehicle, i.e. [Effects.CreateVehicleToken] —
 *    the same predefined token Mu Yanling, Wind Rider creates.
 *  - **The −7 emblem** is a *triggered* emblem, so it uses
 *    [Effects.CreateGlobalTriggeredAbility] with [Duration.Permanent] (the Tezzeret, Cruel Captain
 *    shape) rather than `CreatePermanentEmblem`, which only carries static P/T and keyword
 *    modifications. Global grants aren't attached to an entity and aren't zone-checked, so the
 *    emblem outlives Chandra. Emblems are colorless (printed ruling), so a permanent with
 *    protection from red is still a legal target for its damage.
 */
val ChandraSparkHunter = card("Chandra, Spark Hunter") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 4
    oracleText = "At the beginning of combat on your turn, choose up to one target Vehicle you " +
        "control. Until end of turn, it becomes an artifact creature and gains haste.\n" +
        "+2: You may sacrifice an artifact or discard a card. If you do, draw a card.\n" +
        "0: Create a 3/2 colorless Vehicle artifact token with crew 1.\n" +
        "−7: You get an emblem with \"Whenever an artifact you control enters, this emblem deals " +
        "3 damage to any target.\""

    // At the beginning of combat on your turn, choose up to one target Vehicle you control.
    // Until end of turn, it becomes an artifact creature and gains haste.
    triggeredAbility {
        trigger = Triggers.BeginCombat
        val vehicle = target(
            "up to one target Vehicle you control",
            TargetPermanent(
                optional = true,
                filter = TargetFilter(GameObjectFilter.Any.withSubtype(Subtype.VEHICLE).youControl())
            )
        )
        effect = Effects.AddCardType("Creature", vehicle, Duration.EndOfTurn) then
            Effects.GrantKeyword(Keyword.HASTE, vehicle, Duration.EndOfTurn)
    }

    // +2: You may sacrifice an artifact or discard a card. If you do, draw a card.
    loyaltyAbility(+2) {
        effect = MayEffect(
            effect = Effects.ChooseAction(
                choices = listOf(
                    EffectChoice(
                        label = "Sacrifice an artifact",
                        effect = Effects.Sacrifice(
                            GameObjectFilter.Artifact,
                            1,
                            EffectTarget.Controller
                        ) then Effects.DrawCards(1),
                        feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                            GameObjectFilter.Artifact
                        )
                    ),
                    EffectChoice(
                        label = "Discard a card",
                        effect = Patterns.Hand.discardCards(1) then Effects.DrawCards(1),
                        feasibilityCheck = FeasibilityCheck.HasCardsInZone(Zone.HAND)
                    )
                )
            ),
            descriptionOverride = "You may sacrifice an artifact or discard a card. If you do, " +
                "draw a card."
        )
    }

    // 0: Create a 3/2 colorless Vehicle artifact token with crew 1.
    loyaltyAbility(0) {
        effect = Effects.CreateVehicleToken()
    }

    // −7: You get an emblem with "Whenever an artifact you control enters, this emblem deals
    //     3 damage to any target."
    loyaltyAbility(-7) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.entersBattlefield(
                    filter = GameObjectFilter.Artifact.youControl(),
                    binding = TriggerBinding.ANY
                ).event,
                binding = TriggerBinding.ANY,
                effect = Effects.DealDamage(3, EffectTarget.ContextTarget(0)),
                targetRequirement = Targets.Any,
                descriptionOverride = "Whenever an artifact you control enters, this emblem deals " +
                    "3 damage to any target."
            ),
            descriptionOverride = "Whenever an artifact you control enters, this emblem deals 3 " +
                "damage to any target."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "116"
        artist = "Devin Elle Kurtz"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11f9b98e-48a1-491e-bdab-6e94e4ec747a.jpg?1783907885"

        ruling("2025-02-07", "Emblems are colorless. This means that a permanent with protection from red can be the target of the emblem's triggered ability.")
    }
}

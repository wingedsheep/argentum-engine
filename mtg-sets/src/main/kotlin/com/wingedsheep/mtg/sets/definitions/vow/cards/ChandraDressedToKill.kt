package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayerOrPlaneswalker
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Chandra, Dressed to Kill — Innistrad: Crimson Vow #149
 * {1}{R}{R} · Legendary Planeswalker — Chandra · Starting loyalty 3
 *
 * +1: Add {R}. Chandra deals 1 damage to up to one target player or planeswalker.
 * +1: Exile the top card of your library. If it's red, you may cast it this turn.
 * −7: Exile the top five cards of your library. You may cast red spells from among them this turn.
 *     You get an emblem with "Whenever you cast a red spell, this emblem deals X damage to any
 *     target, where X is the amount of mana spent to cast that spell."
 *
 * Modeling notes:
 *
 *  - **The first +1** is an ordinary loyalty ability that happens to add mana, not a mana ability —
 *    it targets and so uses the stack (printed ruling). "Up to one target" is `optional = true`;
 *    with no target chosen the mana is still added.
 *  - **The two "cast from exile" clauses differ, and the rulings are explicit about how.** The
 *    middle +1 tests the *exiled card's* characteristics ("If it's **red**"), so the pipeline
 *    filters the exiled collection with [CollectionFilter.MatchesFilter] before granting — a red
 *    modal double-faced card qualifies and *either* face may then be cast. The −7 instead tests the
 *    *spell* ("you may cast red **spells**"), which is the permission-level
 *    `castColorRestriction`: all five cards are granted, but the check runs against the face being
 *    cast, so the blue back face of that same MDFC is not castable this way.
 *  - Both grants are `nonLandOnly` — "cast" never covers a land's play-as-a-special-action
 *    (CR 305.1) — and neither waives a cost, matching "You must pay all costs for spells cast via
 *    Chandra's last two abilities."
 *  - **The emblem** is a triggered emblem ([Effects.CreateGlobalTriggeredAbility], the Chandra,
 *    Spark Hunter shape) so it outlives Chandra. X reads the mana *actually* spent on the
 *    triggering spell — `EntityProperty(Triggering, ManaSpent)`, which sums the per-colour tallies
 *    recorded on the spell as it was cast, so cost increases and reductions are already baked in.
 */
val ChandraDressedToKill = card("Chandra, Dressed to Kill") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 3
    oracleText = "+1: Add {R}. Chandra deals 1 damage to up to one target player or planeswalker.\n" +
        "+1: Exile the top card of your library. If it's red, you may cast it this turn.\n" +
        "−7: Exile the top five cards of your library. You may cast red spells from among them " +
        "this turn. You get an emblem with \"Whenever you cast a red spell, this emblem deals X " +
        "damage to any target, where X is the amount of mana spent to cast that spell.\""

    // +1: Add {R}. Chandra deals 1 damage to up to one target player or planeswalker.
    loyaltyAbility(+1) {
        val victim = target(
            "up to one target player or planeswalker",
            TargetPlayerOrPlaneswalker(optional = true)
        )
        effect = Effects.AddMana(Color.RED, 1) then Effects.DealDamage(1, victim)
    }

    // +1: Exile the top card of your library. If it's red, you may cast it this turn.
    loyaltyAbility(+1) {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                    storeAs = "chandraExiled"
                ),
                MoveCollectionEffect(
                    from = "chandraExiled",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                // "If it's red" is a property of the exiled *card*, so it gates the grant itself.
                FilterCollectionEffect(
                    from = "chandraExiled",
                    filter = CollectionFilter.MatchesFilter(GameObjectFilter.Any.withColor(Color.RED)),
                    storeMatching = "chandraExiledRed"
                ),
                Effects.GrantMayPlayFromExile("chandraExiledRed", nonLandOnly = true),
            )
        )
        description = "Exile the top card of your library. If it's red, you may cast it this turn."
    }

    // −7: Exile the top five, cast red spells among them this turn, and get the damage emblem.
    loyaltyAbility(-7) {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(5)),
                    storeAs = "chandraExiledFive"
                ),
                MoveCollectionEffect(
                    from = "chandraExiledFive",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                // "red spells" — checked against the spell as it is cast, not the exiled card.
                Effects.GrantMayPlayFromExile(
                    from = "chandraExiledFive",
                    nonLandOnly = true,
                    castColorRestriction = Color.RED,
                ),
                Effects.CreateGlobalTriggeredAbility(
                    ability = TriggeredAbility.create(
                        trigger = Triggers.youCastSpell(
                            spellFilter = GameObjectFilter.Any.withColor(Color.RED)
                        ).event,
                        binding = TriggerBinding.ANY,
                        effect = Effects.DealDamage(
                            DynamicAmount.EntityProperty(
                                EntityReference.Triggering,
                                EntityNumericProperty.ManaSpent
                            ),
                            EffectTarget.ContextTarget(0)
                        ),
                        targetRequirement = Targets.Any,
                        descriptionOverride = "Whenever you cast a red spell, this emblem deals X " +
                            "damage to any target, where X is the amount of mana spent to cast that spell."
                    ),
                    descriptionOverride = "Whenever you cast a red spell, this emblem deals X damage " +
                        "to any target, where X is the amount of mana spent to cast that spell."
                ),
            )
        )
        description = "Exile the top five cards of your library. You may cast red spells from among " +
            "them this turn. You get an emblem with \"Whenever you cast a red spell, this emblem " +
            "deals X damage to any target, where X is the amount of mana spent to cast that spell.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "149"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/681f7c73-92c6-47ba-af56-3ff032ac12da.jpg?1783924841"

        ruling("2025-01-24", "Chandra's first ability uses the stack and can be responded to, even if no targets were chosen. It isn't a mana ability.")
        ruling("2025-01-24", "Chandra's second ability checks the characteristics of the card you exiled to see if it's red. If the card is red, but the spell that card becomes somehow isn't red, you may still cast it. For example, Rowan, Scholar of Sparks, the front face of a modal double-faced card, is red. Will, Scholar of Frost, the back face of the same card, is blue. In exile, that card is red, so you may cast either face if you exile it with this ability.")
        ruling("2025-01-24", "Chandra's last ability, by contrast, only allows you to cast spells that are red when you cast them, no matter what the card in exile is. If Rowan, Scholar of Sparks (a modal double-faced card) is exiled by this ability, you won't be able to cast its back face, Will, Scholar of Frost, this way because it would be a blue spell.")
        ruling("2025-01-24", "You must pay all costs for spells cast via Chandra's last two abilities. For the middle ability, you must also follow all timing restrictions.")
        ruling("2025-01-24", "The emblem's triggered ability looks for the actual amount of mana spent to cast the spell. If an effect caused you to pay more or less mana for that spell as you cast it, that will be taken into account when determining the value of X. If an effect would counter that spell unless you pay some amount of mana, that mana doesn't count as mana spent to cast it.")
    }
}

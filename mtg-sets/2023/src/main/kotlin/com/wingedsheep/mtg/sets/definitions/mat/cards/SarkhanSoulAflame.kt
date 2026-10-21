package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sarkhan, Soul Aflame
 * {1}{U}{R}
 * Legendary Creature — Human Shaman
 * 2/4
 *
 * Dragon spells you cast cost {1} less to cast.
 * Whenever a Dragon you control enters, you may have Sarkhan become a copy of it until end of
 * turn, except its name is Sarkhan, Soul Aflame and it's legendary in addition to its other types.
 *
 * The copy is [Effects.EachPermanentBecomesCopyOfTarget] with `affected = Self` and the entering
 * Dragon as `TriggeringEntity` (read live, or as it last existed on the battlefield if it has left
 * — CR 707.2), reverted at cleanup; the name and legendary supertype ride as copiable exceptions
 * (CR 707.9b).
 */
val SarkhanSoulAflame = card("Sarkhan, Soul Aflame") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Shaman"
    power = 2
    toughness = 4
    oracleText = "Dragon spells you cast cost {1} less to cast.\n" +
        "Whenever a Dragon you control enters, you may have Sarkhan become a copy of it until end " +
        "of turn, except its name is Sarkhan, Soul Aflame and it's legendary in addition to its " +
        "other types."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype("Dragon")),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl().withSubtype(Subtype.DRAGON)).enters()
        effect = Effects.May(
            Effects.EachPermanentBecomesCopyOfTarget(
                target = EffectTarget.TriggeringEntity,
                affected = EffectTarget.Self,
                duration = Duration.EndOfTurn,
                exceptions = CopyExceptions(
                    nameOverride = "Sarkhan, Soul Aflame",
                    addedSupertypes = setOf(Supertype.LEGENDARY),
                ),
            ),
            prompt = "Have Sarkhan become a copy of that Dragon until end of turn?",
        )
        description = "Whenever a Dragon you control enters, you may have Sarkhan become a copy " +
            "of it until end of turn, except its name is Sarkhan, Soul Aflame and it's legendary " +
            "in addition to its other types."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "46"
        artist = "Eric Wilkerson"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/6225f139-f6eb-4eb5-9776-159a599d8255.jpg?1783916508"

        ruling("2023-05-12", "Sarkhan's first ability doesn't change the mana cost or mana value of any Dragon spell. It changes only the total cost you pay.")
        ruling("2023-05-12", "That ability can't reduce the amount of colored mana you pay for a Dragon spell. It reduces only the generic component of that cost.")
        ruling("2023-05-12", "Sarkhan copies exactly what was printed on the original Dragon and nothing else, with the noted exceptions (unless that creature is a token; see below). It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on in response to Sarkhan's ability.")
        ruling("2023-05-12", "If the copied Dragon has {X} in its mana cost, X is 0.")
        ruling("2023-05-12", "If the copied Dragon is a token, Sarkhan copies the original characteristics of that token as stated by the effect that created that token, with the noted exceptions.")
        ruling("2023-05-12", "In some unusual cases, the Dragon that entered the battlefield will be copying something else by the time Sarkhan's ability resolves. In such a case, Sarkhan will copy whatever the Dragon is copying, with the noted exceptions.")
        ruling("2023-05-12", "If the Dragon that enters the battlefield is legendary and you have Sarkhan copy it, the two Dragons will have different names (unless the entering Dragon is somehow named Sarkhan, Soul Aflame). The \"legend rule\" won't apply.")
    }
}

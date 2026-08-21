package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Slag Strider — Modern Horizons 2 #141
 * {5}{R}{R} · Creature — Elemental · 3 / 3
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * {1}, Sacrifice an artifact: This creature deals 1 damage to any target.
 *
 * Pure composition. [KeywordAbility.Affinity] for [CardType.ARTIFACT] is engine-live vocabulary —
 * the cost calculator reads the *KeywordAbility*, never a `Keyword.AFFINITY` enum entry, so the
 * printed keyword line is left for `CardBuilder.build()` to derive. Affinity only shaves generic
 * mana, so this floors at {R}{R} no matter how wide the artifact board gets.
 *
 * The activated ability is a two-atom [Costs.Composite]: the mana and the sacrifice are both costs,
 * so they are paid on activation and the ability is on the stack independent of the artifact it ate.
 * [Costs.Sacrifice] (not `SacrificeAnother`) is deliberate — the Strider is *not* an artifact, so
 * "an artifact" can never mean itself here, and the printed text carries no "another".
 */
val SlagStrider = card("Slag Strider") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 3
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "{1}, Sacrifice an artifact: This creature deals 1 damage to any target."

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Sacrifice(GameObjectFilter.Artifact))
        val t = target("target", AnyTarget())
        effect = Effects.DealDamage(1, t)
        description = "{1}, Sacrifice an artifact: This creature deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Yeong-Hao Han"
        flavorText = "Imbued with life by a stray bolt of spellcraft, it began to wander about the foundry, to the blacksmiths' alarm."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3a271f3-ea5f-4947-aac3-b4cffdfa87ac.jpg?1783926838"
    }
}

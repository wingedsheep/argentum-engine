package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lilah, Undefeated Slickshot
 * {1}{U}{R}
 * Legendary Creature — Human Rogue
 * 3/3
 *
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * Whenever you cast a multicolored instant or sorcery spell from your hand, exile that spell
 * instead of putting it into your graveyard as it resolves. If you do, it becomes plotted.
 * (You may cast it as a sorcery on a later turn without paying its mana cost.)
 *
 * Implementation:
 * - Prowess via the [prowess] card-builder helper (adds the keyword + the noncreature-cast +1/+1
 *   trigger).
 * - The second ability is a triggered ability shaped exactly like Goliath Daydreamer's
 *   "exile that card … instead of putting it into your graveyard as it resolves" — it marks the
 *   triggering spell with [Effects.MarkSpellPlotOnResolve] (the plot sibling of
 *   MarkSpellExileWithCounters). The spell resolves fully; only its post-resolution destination is
 *   re-routed to exile, where it becomes plotted for its owner. Per the engine's `onlyIfResolved`
 *   semantics, if the spell is countered or otherwise fails to resolve it goes to the graveyard
 *   normally — matching the "If you do" clause.
 */
val LilahUndefeatedSlickshot = card("Lilah, Undefeated Slickshot") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Rogue"
    power = 3
    toughness = 3
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until " +
        "end of turn.)\n" +
        "Whenever you cast a multicolored instant or sorcery spell from your hand, exile that " +
        "spell instead of putting it into your graveyard as it resolves. If you do, it becomes " +
        "plotted. (You may cast it as a sorcery on a later turn without paying its mana cost.)"

    prowess()

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.InstantOrSorcery and GameObjectFilter.Multicolored,
            requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)),
        )
        effect = Effects.MarkSpellPlotOnResolve(target = EffectTarget.TriggeringEntity)
        description = "Whenever you cast a multicolored instant or sorcery spell from your hand, " +
            "exile that spell instead of putting it into your graveyard as it resolves. It becomes plotted."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e21f90ea-5934-4757-8515-38ef116afac1.jpg?1712356151"

        ruling("2024-04-12", "Lilah's last ability triggers when you cast a multicolored instant or sorcery spell from your hand. It doesn't matter how the spell resolves; if it resolves, it's exiled and becomes plotted instead of being put into your graveyard.")
        ruling("2024-04-12", "If the spell is countered or otherwise fails to resolve, it isn't exiled and doesn't become plotted. It's put into its owner's graveyard.")
        ruling("2024-04-12", "You can't cast a plotted card the same turn it became plotted.")
    }
}

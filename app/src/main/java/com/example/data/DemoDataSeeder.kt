package com.example.data

object DemoDataSeeder {

    const val VK_COMMUNITY_URL = "https://vk.ru/club241842043"
    private const val HOUR_MS = 3_600_000L

    fun createInitialState(nowMillis: Long = System.currentTimeMillis()): CrmDatabaseState {
        val programs = listOf(
            Program(
                id = "prog_online",
                title = "Онлайн-курс «Здоровый сон»",
                format = ProgramFormat.ONLINE,
                priceRub = 14000,
                durationDesc = "8 модулей · бессрочный доступ",
                modulesCount = 8,
                installmentAllowed = true,
                archived = false,
                description = "Пошаговое восстановление циркадных ритмов и преодоление реваншистской прокрастинации сна с поддержкой куратора в приложении."
            ),
            Program(
                id = "prog_group",
                title = "Групповой выездной интенсив на турбазе",
                format = ProgramFormat.GROUP_TOURBASE,
                priceRub = 28000,
                durationDesc = "3 дня · Волгоградская область",
                modulesCount = 6,
                installmentAllowed = true,
                archived = false,
                description = "Цифровой детокс в сосновом лесу у Волги или Дона, вечерние практики релаксации и настройка режима сна в малой группе."
            ),
            Program(
                id = "prog_vip",
                title = "Индивидуальный VIP-интенсив (медобразование)",
                format = ProgramFormat.VIP_TOURBASE,
                priceRub = 48000,
                durationDesc = "3 дня · персональный эксперт",
                modulesCount = 9,
                installmentAllowed = true,
                archived = false,
                description = "Индивидуальное сопровождение специалиста с медицинским образованием, глубокая диагностика усталости нервной системы и личный план восстановления."
            ),
            Program(
                id = "prog_express_club",
                title = "Клуб поддержки сна «Тихий вечер»",
                format = ProgramFormat.ONLINE,
                priceRub = 6500,
                durationDesc = "4 недели · еженедельные разборы",
                modulesCount = 4,
                installmentAllowed = false,
                archived = false,
                description = "Поддерживающий формат для выпускников базового курса: закрепление привычек гигиены сна и групповые созвоны."
            )
        )

        val specialists = listOf(
            Specialist(
                id = "sp_1",
                fullName = "Елена Сергеевна Морозова",
                specialization = "Сомнолог-консультант, врач-невролог (15 лет практики)",
                hasMedicalEducation = true,
                assignedProgramIds = listOf("prog_vip", "prog_group", "prog_online"),
                workSchedule = "Пн–Пт 09:00–19:00, выезды Пт–Вс",
                rating = 4.96,
                completedSessions = 418,
                phone = "+7 (903) 371-14-20"
            ),
            Specialist(
                id = "sp_2",
                fullName = "Дмитрий Андреевич Волков",
                specialization = "Клинический психолог, эксперт по прокрастинации сна",
                hasMedicalEducation = true,
                assignedProgramIds = listOf("prog_vip", "prog_online"),
                workSchedule = "Вт–Сб 10:00–20:00",
                rating = 4.91,
                completedSessions = 326,
                phone = "+7 (904) 412-88-19"
            ),
            Specialist(
                id = "sp_3",
                fullName = "Ольга Викторовна Савина",
                specialization = "Ведущая выездных интенсивов, коуч гигиены сна",
                hasMedicalEducation = false,
                assignedProgramIds = listOf("prog_group", "prog_online", "prog_express_club"),
                workSchedule = "Ср–Вс 09:30–18:30",
                rating = 4.88,
                completedSessions = 274,
                phone = "+7 (917) 842-55-03"
            ),
            Specialist(
                id = "sp_4",
                fullName = "Артём Игоревич Ковалёв",
                specialization = "Специалист по стресс-менеджменту и циркадным ритмам",
                hasMedicalEducation = false,
                assignedProgramIds = listOf("prog_online", "prog_group"),
                workSchedule = "Пн–Пт 11:00–20:00",
                rating = 4.84,
                completedSessions = 195,
                phone = "+7 (961) 665-39-11"
            )
        )

        val tourbases = listOf(
            Tourbase("tb_1", "«Волжский ветер»", "Среднеахтубинский р-н (Волго-Ахтубинская пойма)", 16, 12, "Ирина Полякова", "+7 (902) 311-44-01", listOf("27–29 ноя", "4–6 дек", "18–20 дек"), true, true, "Групповой интенсив"),
            Tourbase("tb_2", "«Сосновый берег Дона»", "Иловлинский р-н", 12, 12, "Сергей Макаров", "+7 (905) 392-18-02", listOf("11–13 дек", "25–27 дек"), true, true, "Групповой интенсив"),
            Tourbase("tb_3", "«Тихая Ахтуба»", "Ленинский р-н", 10, 4, "Анна Громова", "+7 (917) 338-90-03", listOf("27–29 ноя", "11–13 дек", "15–17 янв"), true, false, "Групповой интенсив"),
            Tourbase("tb_4", "«Дубовские зори»", "Дубовский р-н (Волгоградское водохранилище)", 14, 8, "Павел Чернов", "+7 (904) 774-22-04", listOf("4–6 дек", "18–20 дек", "22–24 янв"), true, true, "Групповой + VIP"),
            Tourbase("tb_5", "«Лебяжья поляна Эко»", "Среднеахтубинский р-н", 6, 3, "Наталья Власова", "+7 (903) 378-65-05", listOf("27–29 ноя", "4–6 дек", "11–13 дек"), false, true, "VIP-интенсив"),
            Tourbase("tb_6", "«Донская излучина»", "Калачёвский р-н", 14, 0, "Виктор Соколов", "+7 (961) 082-13-06", listOf("27–29 ноя", "11–13 дек", "18–20 дек"), true, false, "Групповой интенсив"),
            Tourbase("tb_7", "«Серебряный плёс»", "Городищенский р-н", 8, 5, "Мария Орлова", "+7 (927) 519-40-07", listOf("4–6 дек", "18–20 дек"), true, true, "VIP-интенсив"),
            Tourbase("tb_8", "«Хвойный причал»", "Светлоярский р-н", 12, 6, "Алексей Титов", "+7 (902) 654-71-08", listOf("11–13 дек", "25–27 дек", "15–17 янв"), true, false, "Групповой интенсив"),
            Tourbase("tb_9", "«Медведицкие сосны»", "Даниловский р-н", 10, 0, "Екатерина Лукина", "+7 (909) 388-12-09", listOf("4–6 дек", "18–20 дек", "29–31 янв"), true, true, "Групповой + VIP"),
            Tourbase("tb_10", "«Волжская тишина VIP»", "Среднеахтубинский р-н", 4, 4, "Олег Романов", "+7 (919) 795-00-10", listOf("18–20 дек", "15–17 янв"), false, true, "VIP-интенсив"),
            Tourbase("tb_11", "«Заповедный залив»", "Быковский р-н", 12, 3, "Татьяна Белова", "+7 (937) 701-55-11", listOf("27–29 ноя", "11–13 дек", "22–24 янв"), true, false, "Групповой интенсив"),
            Tourbase("tb_12", "«Эко-хутор Берёзовый»", "Фроловский р-н", 8, 2, "Дмитрий Киселёв", "+7 (904) 409-34-12", listOf("4–6 дек", "25–27 дек", "15–17 янв"), true, true, "Групповой + VIP")
        )

        val clients = listOf(
            Client(
                id = "cl_1",
                fullName = "Марина Александровна Соколова",
                phone = "+7 (902) 314-28-91",
                age = 48,
                segment = AgeSegment.WOMEN_40_59,
                source = LeadSource.MOBILE_APP,
                pdnConsentSigned = true,
                pdnConsentDate = "12.11.2026",
                expressTestScore = 76,
                recommendedProgramId = "prog_group",
                sleepDiary = SleepDiaryProgress(11, 13, 10, 14, 6.4),
                assignedSpecialistId = "sp_1",
                specialistNotes = listOf(
                    SpecialistNote("n1", "sp_1", "Елена Морозова", "16.11.2026", "Сильная вечерняя тревожность и листание ленты до 01:30. Рекомендован ритуал отключения экрана в 22:00 и выезд на «Волжский ветер».")
                ),
                createdAt = "12.11.2026"
            ),
            Client(
                id = "cl_2",
                fullName = "Виктор Петрович Семёнов",
                phone = "+7 (917) 332-19-40",
                age = 66,
                segment = AgeSegment.SENIORS_60_PLUS,
                source = LeadSource.VK,
                pdnConsentSigned = true,
                pdnConsentDate = "10.11.2026",
                expressTestScore = 84,
                recommendedProgramId = "prog_vip",
                sleepDiary = SleepDiaryProgress(12, 14, 13, 14, 6.1),
                assignedSpecialistId = "sp_1",
                specialistNotes = listOf(
                    SpecialistNote("n2", "sp_1", "Елена Морозова", "15.11.2026", "Ранние пробуждения в 04:20. Хороший отклик на дыхательную технику и вечерние прогулки.")
                ),
                createdAt = "10.11.2026"
            ),
            Client(
                id = "cl_3",
                fullName = "Алексей Дмитриевич Громов",
                phone = "+7 (904) 418-77-12",
                age = 34,
                segment = AgeSegment.PROS_25_45,
                source = LeadSource.MOBILE_APP,
                pdnConsentSigned = true,
                pdnConsentDate = "14.11.2026",
                expressTestScore = 68,
                recommendedProgramId = "prog_online",
                sleepDiary = SleepDiaryProgress(6, 10, 7, 14, 5.8),
                assignedSpecialistId = "sp_2",
                specialistNotes = listOf(
                    SpecialistNote("n3", "sp_2", "Дмитрий Волков", "17.11.2026", "Классическая реваншистская прокрастинация сна после рабочих созвонов. Выдали трекер границ рабочего дня.")
                ),
                createdAt = "14.11.2026"
            ),
            Client(
                id = "cl_4",
                fullName = "Светлана Игоревна Карпова",
                phone = "+7 (905) 399-43-15",
                age = 52,
                segment = AgeSegment.WOMEN_40_59,
                source = LeadSource.SITE,
                pdnConsentSigned = false,
                pdnConsentDate = null,
                expressTestScore = 72,
                recommendedProgramId = "prog_group",
                sleepDiary = SleepDiaryProgress(4, 7, 5, 10, 5.5),
                assignedSpecialistId = "sp_3",
                specialistNotes = emptyList(),
                createdAt = "18.11.2026"
            ),
            Client(
                id = "cl_5",
                fullName = "Галина Николаевна Фёдорова",
                phone = "+7 (927) 510-88-24",
                age = 68,
                segment = AgeSegment.SENIORS_60_PLUS,
                source = LeadSource.VK,
                pdnConsentSigned = true,
                pdnConsentDate = "05.11.2026",
                expressTestScore = 62,
                recommendedProgramId = "prog_online",
                sleepDiary = SleepDiaryProgress(13, 14, 12, 14, 7.2),
                assignedSpecialistId = "sp_3",
                specialistNotes = listOf(
                    SpecialistNote("n4", "sp_3", "Ольга Савина", "14.11.2026", "Стабильное улучшение засыпания: с 50 минут сократилось до 15 минут. Готова к выпускному модулю.")
                ),
                createdAt = "05.11.2026"
            ),
            Client(
                id = "cl_6",
                fullName = "Денис Олегович Румянцев",
                phone = "+7 (961) 660-21-54",
                age = 29,
                segment = AgeSegment.PROS_25_45,
                source = LeadSource.MOBILE_APP,
                pdnConsentSigned = false,
                pdnConsentDate = null,
                expressTestScore = 59,
                recommendedProgramId = "prog_online",
                sleepDiary = SleepDiaryProgress(3, 6, 4, 10, 5.4),
                assignedSpecialistId = "sp_4",
                specialistNotes = emptyList(),
                createdAt = "19.11.2026"
            ),
            Client(
                id = "cl_7",
                fullName = "Наталья Владимировна Ермакова",
                phone = "+7 (903) 376-15-82",
                age = 44,
                segment = AgeSegment.WOMEN_40_59,
                source = LeadSource.MOBILE_APP,
                pdnConsentSigned = true,
                pdnConsentDate = "08.11.2026",
                expressTestScore = 88,
                recommendedProgramId = "prog_vip",
                sleepDiary = SleepDiaryProgress(9, 12, 10, 14, 6.6),
                assignedSpecialistId = "sp_1",
                specialistNotes = listOf(
                    SpecialistNote("n5", "sp_1", "Елена Морозова", "17.11.2026", "Выраженное эмоциональное истощение. На VIP-интенсиве в «Лебяжьей поляне» запланирован блок восстановления вегетатики.")
                ),
                createdAt = "08.11.2026"
            ),
            Client(
                id = "cl_8",
                fullName = "Игорь Сергеевич Платонов",
                phone = "+7 (919) 791-62-09",
                age = 38,
                segment = AgeSegment.PROS_25_45,
                source = LeadSource.SITE,
                pdnConsentSigned = true,
                pdnConsentDate = "11.11.2026",
                expressTestScore = 79,
                recommendedProgramId = "prog_group",
                sleepDiary = SleepDiaryProgress(8, 11, 9, 14, 6.2),
                assignedSpecialistId = "sp_2",
                specialistNotes = emptyList(),
                createdAt = "11.11.2026"
            ),
            Client(
                id = "cl_9",
                fullName = "Тамара Васильевна Кузнецова",
                phone = "+7 (902) 650-19-33",
                age = 71,
                segment = AgeSegment.SENIORS_60_PLUS,
                source = LeadSource.VK,
                pdnConsentSigned = true,
                pdnConsentDate = "02.11.2026",
                expressTestScore = 70,
                recommendedProgramId = "prog_group",
                sleepDiary = SleepDiaryProgress(14, 14, 13, 14, 7.4),
                assignedSpecialistId = "sp_3",
                specialistNotes = listOf(
                    SpecialistNote("n6", "sp_3", "Ольга Савина", "12.11.2026", "Успешно завершила 3-дневный интенсив в «Дубовских зорях». Сон ровный, без ночных подъёмов.")
                ),
                createdAt = "02.11.2026"
            ),
            Client(
                id = "cl_10",
                fullName = "Екатерина Романовна Беляева",
                phone = "+7 (937) 712-44-67",
                age = 41,
                segment = AgeSegment.WOMEN_40_59,
                source = LeadSource.MOBILE_APP,
                pdnConsentSigned = true,
                pdnConsentDate = "15.11.2026",
                expressTestScore = 65,
                recommendedProgramId = "prog_online",
                sleepDiary = SleepDiaryProgress(10, 11, 9, 14, 6.7),
                assignedSpecialistId = "sp_4",
                specialistNotes = emptyList(),
                createdAt = "15.11.2026"
            ),
            Client(
                id = "cl_11",
                fullName = "Павел Евгеньевич Тихонов",
                phone = "+7 (904) 405-98-11",
                age = 31,
                segment = AgeSegment.PROS_25_45,
                source = LeadSource.VK,
                pdnConsentSigned = true,
                pdnConsentDate = "16.11.2026",
                expressTestScore = 54,
                recommendedProgramId = "prog_online",
                sleepDiary = SleepDiaryProgress(7, 8, 6, 14, 6.0),
                assignedSpecialistId = "sp_4",
                specialistNotes = emptyList(),
                createdAt = "16.11.2026"
            ),
            Client(
                id = "cl_12",
                fullName = "Людмила Григорьевна Зайцева",
                phone = "+7 (905) 334-12-80",
                age = 57,
                segment = AgeSegment.WOMEN_40_59,
                source = LeadSource.SITE,
                pdnConsentSigned = true,
                pdnConsentDate = "01.11.2026",
                expressTestScore = 81,
                recommendedProgramId = "prog_vip",
                sleepDiary = SleepDiaryProgress(10, 13, 11, 14, 6.9),
                assignedSpecialistId = "sp_2",
                specialistNotes = listOf(
                    SpecialistNote("n7", "sp_2", "Дмитрий Волков", "13.11.2026", "Попросила перенос интенсивной программы на декабрь по семейным обстоятельствам.")
                ),
                createdAt = "01.11.2026"
            )
        )

        val deals = listOf(
            Deal("dl_1", "cl_6", "prog_online", 14000, DealStage.NEW_LEAD, false, null, "sp_4", "19.11.2026", "19.11.2026", "Заявка из мобильного приложения, ждёт звонка"),
            Deal("dl_2", "cl_4", "prog_group", 28000, DealStage.TEST_PASSED, false, "tb_1", "sp_3", "18.11.2026", "18.11.2026", "Тест 72 балла, требуется подписать согласие 152-ФЗ"),
            Deal("dl_3", "cl_11", "prog_online", 14000, DealStage.CONSULTATION, true, null, "sp_4", "16.11.2026", "18.11.2026", "Интересуется рассрочкой 0% на 2 месяца"),
            Deal("dl_4", "cl_8", "prog_group", 28000, DealStage.AWAITING_PAYMENT, true, "tb_4", "sp_2", "11.11.2026", "18.11.2026", "Выбрал «Дубовские зори» 4–6 дек, выставлен график 0%"),
            Deal("dl_5", "cl_1", "prog_group", 28000, DealStage.PAID_BOOKED, false, "tb_1", "sp_1", "12.11.2026", "17.11.2026", "Оплачено полностью, заезд 27–29 ноя на «Волжский ветер»"),
            Deal("dl_6", "cl_7", "prog_vip", 48000, DealStage.PAID_BOOKED, true, "tb_5", "sp_1", "08.11.2026", "17.11.2026", "VIP-интенсив на «Лебяжья поляна Эко», рассрочка 0%"),
            Deal("dl_7", "cl_2", "prog_vip", 48000, DealStage.IN_PROGRESS, false, "tb_10", "sp_1", "10.11.2026", "18.11.2026", "Проходит индивидуальное сопровождение у Е. Морозовой"),
            Deal("dl_8", "cl_3", "prog_online", 14000, DealStage.IN_PROGRESS, true, null, "sp_2", "14.11.2026", "18.11.2026", "Модуль 4 из 8, автооплата из приложения"),
            Deal("dl_9", "cl_10", "prog_online", 14000, DealStage.IN_PROGRESS, false, null, "sp_4", "15.11.2026", "18.11.2026", "Активно ведёт дневник сна в приложении"),
            Deal("dl_10", "cl_5", "prog_online", 14000, DealStage.COMPLETED, false, null, "sp_3", "05.11.2026", "16.11.2026", "Курс успешно завершён, оставлен отзыв 5★"),
            Deal("dl_11", "cl_9", "prog_group", 28000, DealStage.COMPLETED, false, "tb_4", "sp_3", "02.11.2026", "13.11.2026", "Завершила выездной интенсив, отличный результат"),
            Deal("dl_12", "cl_12", "prog_vip", 48000, DealStage.CANCELLED, false, "tb_7", "sp_2", "01.11.2026", "13.11.2026", "Перенесено на депозит по просьбе клиента")
        )

        val bookings = listOf(
            Booking(
                id = "bk_1",
                clientId = "cl_1",
                specialistId = "sp_1",
                programId = "prog_group",
                tourbaseId = "tb_1",
                dealId = "dl_5",
                dateLabel = "20.11.2026",
                dayOfWeek = "Пт",
                timeSlot = "11:00",
                scheduledEpochMillis = nowMillis + 14 * HOUR_MS, // < 24 hours: demonstrates 24h warning rule!
                status = BookingStatus.CONFIRMED,
                isPaid = true,
                notes = "Установочная сессия перед выездом (менее 24ч до старта!)"
            ),
            Booking(
                id = "bk_2",
                clientId = "cl_7",
                specialistId = "sp_1",
                programId = "prog_vip",
                tourbaseId = "tb_5",
                dealId = "dl_6",
                dateLabel = "21.11.2026",
                dayOfWeek = "Сб",
                timeSlot = "14:00",
                scheduledEpochMillis = nowMillis + 44 * HOUR_MS,
                status = BookingStatus.CONFIRMED,
                isPaid = true,
                notes = "VIP-диагностика и план восстановления"
            ),
            Booking(
                id = "bk_3",
                clientId = "cl_3",
                specialistId = "sp_2",
                programId = "prog_online",
                tourbaseId = null,
                dealId = "dl_8",
                dateLabel = "21.11.2026",
                dayOfWeek = "Сб",
                timeSlot = "16:00",
                scheduledEpochMillis = nowMillis + 46 * HOUR_MS,
                status = BookingStatus.CONFIRMED,
                isPaid = true,
                notes = "Разбор дневника гигиены сна (Модуль 4)"
            ),
            Booking(
                id = "bk_4",
                clientId = "cl_8",
                specialistId = "sp_2",
                programId = "prog_group",
                tourbaseId = "tb_4",
                dealId = "dl_4",
                dateLabel = "22.11.2026",
                dayOfWeek = "Вс",
                timeSlot = "11:00",
                scheduledEpochMillis = nowMillis + 68 * HOUR_MS,
                status = BookingStatus.CONFIRMED,
                isPaid = false,
                notes = "Подбор программы и знакомство с группой"
            ),
            Booking(
                id = "bk_5",
                clientId = "cl_5",
                specialistId = "sp_3",
                programId = "prog_online",
                tourbaseId = null,
                dealId = "dl_10",
                dateLabel = "23.11.2026",
                dayOfWeek = "Пн",
                timeSlot = "10:00",
                scheduledEpochMillis = nowMillis + 92 * HOUR_MS,
                status = BookingStatus.CONFIRMED,
                isPaid = true,
                notes = "Итоговая сессия и закрепление режима"
            ),
            Booking(
                id = "bk_6",
                clientId = "cl_10",
                specialistId = "sp_4",
                programId = "prog_online",
                tourbaseId = null,
                dealId = "dl_9",
                dateLabel = "24.11.2026",
                dayOfWeek = "Вт",
                timeSlot = "18:00",
                scheduledEpochMillis = nowMillis + 120 * HOUR_MS,
                status = BookingStatus.CONFIRMED,
                isPaid = true,
                notes = "Практика вечерней разгрузки нервной системы"
            )
        )

        val vkProducts = listOf(
            VkProduct("vkp_1", "Утяжелённое одеяло «Волжский сон» (7 кг)", "Текстиль для сна", 6900, 14, 38, VK_COMMUNITY_URL),
            VkProduct("vkp_2", "Шёлковая светонепроницаемая маска 3D", "Аксессуары", 1850, 29, 94, VK_COMMUNITY_URL),
            VkProduct("vkp_3", "Печатный дневник гигиены сна «Кринж & Co»", "Полиграфия", 1200, 42, 112, VK_COMMUNITY_URL),
            VkProduct("vkp_4", "Вечерний травяной сбор «Пойма» (мелисса, лаванда)", "Фитосборы", 890, 8, 67, VK_COMMUNITY_URL),
            VkProduct("vkp_5", "Янтарная прикроватная лампа 1800K без синего спектра", "Освещение", 4200, 11, 29, VK_COMMUNITY_URL)
        )

        val vkOrders = listOf(
            VkOrder("vko_1", "cl_1", "vkp_1", 1, 6900, "16.11.2026", "Доставлен"),
            VkOrder("vko_2", "cl_2", "vkp_4", 2, 1780, "15.11.2026", "Отправлен"),
            VkOrder("vko_3", "cl_3", "vkp_5", 1, 4200, "18.11.2026", "Новый"),
            VkOrder("vko_4", "cl_9", "vkp_2", 1, 1850, "11.11.2026", "Доставлен"),
            VkOrder("vko_5", "cl_5", "vkp_3", 1, 1200, "09.11.2026", "Доставлен")
        )

        val payments = listOf(
            PaymentRecord(
                id = "pay_1",
                dealId = "dl_5",
                clientId = "cl_1",
                programTitle = "Групповой выездной интенсив на турбазе",
                totalAmountRub = 28000,
                paidAmountRub = 28000,
                status = PaymentStatus.PAID,
                date = "17.11.2026",
                isAutoFromApp = true,
                installments = emptyList()
            ),
            PaymentRecord(
                id = "pay_2",
                dealId = "dl_6",
                clientId = "cl_7",
                programTitle = "Индивидуальный VIP-интенсив (медобразование)",
                totalAmountRub = 48000,
                paidAmountRub = 24000,
                status = PaymentStatus.INSTALLMENT,
                date = "10.11.2026",
                isAutoFromApp = false,
                installments = listOf(
                    InstallmentStep("ins_1", 1, "10.11.2026", 12000, InstallmentStepStatus.PAID),
                    InstallmentStep("ins_2", 2, "17.11.2026", 12000, InstallmentStepStatus.PAID),
                    InstallmentStep("ins_3", 3, "24.11.2026", 12000, InstallmentStepStatus.UPCOMING),
                    InstallmentStep("ins_4", 4, "08.12.2026", 12000, InstallmentStepStatus.UPCOMING)
                )
            ),
            PaymentRecord(
                id = "pay_3",
                dealId = "dl_8",
                clientId = "cl_3",
                programTitle = "Онлайн-курс «Здоровый сон»",
                totalAmountRub = 14000,
                paidAmountRub = 7000,
                status = PaymentStatus.OVERDUE,
                date = "01.11.2026",
                isAutoFromApp = true,
                installments = listOf(
                    InstallmentStep("ins_5", 1, "01.11.2026", 7000, InstallmentStepStatus.PAID),
                    InstallmentStep("ins_6", 2, "16.11.2026", 7000, InstallmentStepStatus.OVERDUE)
                )
            ),
            PaymentRecord(
                id = "pay_4",
                dealId = "dl_7",
                clientId = "cl_2",
                programTitle = "Индивидуальный VIP-интенсив (медобразование)",
                totalAmountRub = 48000,
                paidAmountRub = 48000,
                status = PaymentStatus.PAID,
                date = "10.11.2026",
                isAutoFromApp = false,
                installments = emptyList()
            ),
            PaymentRecord(
                id = "pay_5",
                dealId = "dl_10",
                clientId = "cl_5",
                programTitle = "Онлайн-курс «Здоровый сон»",
                totalAmountRub = 14000,
                paidAmountRub = 14000,
                status = PaymentStatus.PAID,
                date = "05.11.2026",
                isAutoFromApp = true,
                installments = emptyList()
            ),
            PaymentRecord(
                id = "pay_6",
                dealId = "dl_12",
                clientId = "cl_12",
                programTitle = "Индивидуальный VIP-интенсив (медобразование)",
                totalAmountRub = 48000,
                paidAmountRub = 48000,
                status = PaymentStatus.DEPOSIT,
                date = "13.11.2026",
                isAutoFromApp = false,
                installments = emptyList()
            )
        )

        val tickets = listOf(
            SupportTicket(
                id = "tk_1",
                clientId = "cl_1",
                subject = "Трансфер до турбазы «Волжский ветер»",
                status = TicketStatus.NEW,
                lastUpdated = "Сегодня, 11:20",
                messages = listOf(
                    SupportMessage("m1", true, "Здравствуйте! Подскажите, во сколько отправляется трансфер из центра Волгограда 27 ноября?", "Сегодня, 11:20")
                )
            ),
            SupportTicket(
                id = "tk_2",
                clientId = "cl_3",
                subject = "Перенос взноса по рассрочке 0%",
                status = TicketStatus.IN_WORK,
                lastUpdated = "Сегодня, 09:45",
                messages = listOf(
                    SupportMessage("m2", true, "Добрый день, задерживают зарплату на 3 дня, можно оплатить второй платёж 21 ноября?", "Вчера, 21:10"),
                    SupportMessage("m3", false, "Алексей, здравствуйте! Да, рассрочка 0% от центра без штрафов — фиксируем перенос до 21.11.", "Сегодня, 09:45")
                )
            ),
            SupportTicket(
                id = "tk_3",
                clientId = "cl_5",
                subject = "Доступ к 7-му модулю онлайн-курса",
                status = TicketStatus.CLOSED,
                lastUpdated = "16.11.2026",
                messages = listOf(
                    SupportMessage("m4", true, "Не открывалось видео в 7 модуле, помогите пожалуйста.", "16.11.2026 14:00"),
                    SupportMessage("m5", false, "Галина Николаевна, обновили доступ в приложении — проверьте, пожалуйста, всё должно работать.", "16.11.2026 14:15"),
                    SupportMessage("m6", true, "Всё открылось, большое спасибо!", "16.11.2026 14:32")
                )
            )
        )

        val pushCampaigns = listOf(
            PushCampaign(
                id = "ps_1",
                title = "Напоминание о сессии за 24 часа",
                body = "Ждём вас на занятии по восстановлению сна. Бесплатный перенос доступен не позднее чем за 24 часа.",
                targetSegment = null,
                type = PushType.REMINDER_24H,
                sentAt = "18.11.2026 18:00",
                recipientCount = 6
            ),
            PushCampaign(
                id = "ps_2",
                title = "Новые даты выездных интенсивов на Волге",
                body = "Открыта запись на декабрьские 3-дневные интенсивы в Волгоградской области. Рассрочка 0%!",
                targetSegment = AgeSegment.WOMEN_40_59,
                type = PushType.SEGMENT_MANUAL,
                sentAt = "15.11.2026 12:30",
                recipientCount = 4
            )
        )

        val vkBroadcasts = listOf(
            VkBroadcast(
                id = "vb_1",
                title = "Прямой эфир: Как победить прокрастинацию сна в 23:00",
                vkUrl = VK_COMMUNITY_URL,
                scheduledDateTime = "22.11.2026 · 19:00 МСК",
                description = "Елена Морозова разберёт 5 триггеров вечернего залипания в смартфон и ответит на вопросы участников сообщества ВК.",
                pushCreated = true
            ),
            VkBroadcast(
                id = "vb_2",
                title = "Видео-экскурсия по турбазе «Дубовские зори»",
                vkUrl = VK_COMMUNITY_URL,
                scheduledDateTime = "25.11.2026 · 17:30 МСК",
                description = "Показываем номера, сосновый берег и вечернюю программу нашего 3-дневного интенсива.",
                pushCreated = false
            )
        )

        val reviews = listOf(
            Review(
                id = "rv_1",
                clientId = "cl_5",
                specialistId = "sp_3",
                programId = "prog_online",
                rating = 5,
                impression = "Впервые за 6 лет засыпаю без телевизора за 15 минут! Очень тёплая и понятная подача от Ольги Викторовны.",
                remarks = "Хотелось бы побольше аудио-медитаций для прослушивания перед сном.",
                date = "16.11.2026",
                publishedInApp = true
            ),
            Review(
                id = "rv_2",
                clientId = "cl_9",
                specialistId = "sp_3",
                programId = "prog_group",
                rating = 5,
                impression = "Выезд на «Дубовские зори» перевернул мой режим. Три дня без телефона и с прогулками у воды — бесценно!",
                remarks = "Всё организовано на высшем уровне.",
                date = "13.11.2026",
                publishedInApp = true
            ),
            Review(
                id = "rv_3",
                clientId = "cl_2",
                specialistId = "sp_1",
                programId = "prog_vip",
                rating = 5,
                impression = "Елена Сергеевна — профессионал с огромным опытом. Уже после второй сессии перестал просыпаться в 4 утра.",
                remarks = "Удобно, что все рекомендации дублируются в приложении.",
                date = "17.11.2026",
                publishedInApp = false
            ),
            Review(
                id = "rv_4",
                clientId = "cl_3",
                specialistId = "sp_2",
                programId = "prog_online",
                rating = 4,
                impression = "Отличный разбор психологии вечернего стресса. Стал ложиться в 23:15 вместо двух ночи.",
                remarks = "Не хватает тёмного напоминания в 22:00 в виджете.",
                date = "18.11.2026",
                publishedInApp = false
            )
        )

        val auditLog = listOf(
            AuditLogEntry("al_1", "19.11.2026 11:22", UserRole.MANAGER, "Менеджер продаж", "Создана сделка", "Клиент Денис Румянцев · Онлайн-курс «Здоровый сон» (14 000 ₽)"),
            AuditLogEntry("al_2", "18.11.2026 18:00", UserRole.ADMIN, "Руководитель центра", "Отправлена пуш-рассылка", "Напоминание о сессии за 24 часа (6 получателей)"),
            AuditLogEntry("al_3", "17.11.2026 16:40", UserRole.SPECIALIST, "Елена Морозова", "Добавлена заметка после сессии", "Клиент Наталья Ермакова · VIP-интенсив"),
            AuditLogEntry("al_4", "17.11.2026 14:10", UserRole.MANAGER, "Менеджер продаж", "Прикреплена турбаза", "Сделка Марины Соколовой → «Волжский ветер»"),
            AuditLogEntry("al_5", "13.11.2026 12:05", UserRole.MANAGER, "Менеджер продаж", "Отмена с переносом на депозит", "Людмила Зайцева · 48 000 ₽ на депозит, слот освобождён")
        )

        return CrmDatabaseState(
            settings = CrmUiSettings(),
            programs = programs,
            specialists = specialists,
            tourbases = tourbases,
            clients = clients,
            deals = deals,
            bookings = bookings,
            vkProducts = vkProducts,
            vkOrders = vkOrders,
            payments = payments,
            tickets = tickets,
            pushCampaigns = pushCampaigns,
            vkBroadcasts = vkBroadcasts,
            reviews = reviews,
            auditLog = auditLog
        )
    }
}

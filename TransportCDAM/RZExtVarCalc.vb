﻿Module RZExtVarCalc1pt4
    'creates an external variables file for the intrazonal road model, based on a single year's input data and growth factors for the other variables
    '1.2 this version allows capacity changes to be specified
    '1.2 it also disaggregates lane km by road type and specifies vehicle fuel proportions
    '1.3 this version allows input from the database, and makes use of a general strategy file for fuel split values
    '1.3 also now breaks down and calculates the cost variable
    '1.3 now also includes option to impose a workplace parking levy
    '1.3 now includes capacity enhancements automatically
    '1.4 fuel efficiency and cost calculations corrected
    '1.6 Recode to calculate by annual timesteps, the dimension of the parameter array has been increased by one to store previous year's data
    'may need to check if the code works if RdZEneSource is not = "Database"

    Dim RoadInputData As IO.FileStream
    Dim ri As IO.StreamReader
    Dim ExtVarOutputData As IO.FileStream
    Dim ev As IO.StreamWriter
    Dim RoadZoneCapData As IO.FileStream
    Dim rzc As IO.StreamReader
    Dim RoadZoneNewCapData As IO.FileStream
    Dim rzcw As IO.StreamWriter
    Dim rzcn As IO.StreamReader
    Dim stf As IO.StreamReader
    Dim InputRow As String
    Dim OutputRow As String
    Dim InputData() As String
    Dim PopGrowth As Double
    Dim GVAGrowth As Double
    Dim CostGrowth As Double
    Dim CapID As Long
    Dim CapYear As Integer
    Dim MwayKmChange As Double
    Dim RurADKmChange As Double
    Dim RurASKmChange As Double
    Dim RurMinKmChange As Double
    Dim UrbDKmChange As Double
    Dim UrbSKmChange As Double
    Dim ErrorString As String
    Dim FuelEffOld(144, 34), FuelEffNew(34), FuelEffChange(34) As Double
    Dim Year, WPPLStart As Long
    Dim enestring As String
    Dim enearray As String()
    Dim InputCount As Long
    Dim capstring, caparray() As String
    Dim capnum, zonecapcount As Long
    Dim zonecapdetails(13670, 8) As Double
    Dim sortarray(13670) As String
    Dim padzone, padyear As String
    Dim sortedline As String
    Dim splitline() As String
    Dim arraynum As Long
    Dim stratstring As String
    Dim stratarray As String()
    'declaration of parameters in Sub CalcZoneData()
    Dim ZoneID As String
    Dim PopOld(144, 0) As Double
    Dim GVAOld(144, 0) As Double
    Dim CostOld(144, 4) As Double
    Dim PopNew As Double
    Dim GVANew As Double
    Dim CostNew(4) As Double
    Dim LaneKm(144, 0) As Double
    Dim MLaneKm(144, 0) As Double
    Dim RurADLaneKm(144, 0) As Double
    Dim RurASLaneKm(144, 0) As Double
    Dim RurMinLaneKm(144, 0) As Double
    Dim UrbDLaneKm(144, 0) As Double
    Dim UrbSLaneKm(144, 0) As Double
    Dim FuelString As String
    Dim keylookup As String
    Dim newval As Double
    Dim stratcount As Long
    Dim PetOld(144, 0), PetNew, DieOld(144, 0), DieNew, EleOld(144, 0), EleNew, LPGOld(144, 0), LPGNew, CNGOld(144, 0), CNGNew, HydOld(144, 0), HydNew As Double
    Dim PetRat, DieRat, EleRat, LPGRat, CNGRat, HydRat As Double
    Dim VehCosts(4, 9) As Double
    Dim FuelCostPer(4, 9) As Double
    Dim VehFixedCosts(4, 9) As Double
    Dim VehFuelCosts(144, 4, 9) As Double
    Dim PHPerOld(144, 4), PHPerNew(4)
    Dim UrbRoadPer, WPPLTripPer As Double
    Dim CarbCharge(4, 9) As Double


    Public Sub RoadZoneEVMain()


        'if using WPPL then check if the start year is a valid value
        If WPPL = True Then
            If WPPLYear < 2011 Then
                MsgBox("Invalid start year provided for WPPL.  Please rerun the model using a year between 2011 and 2100.")
                LogLine = "Invalid start year provided for WPPL.  Run terminated during intrazonal road model external variable file generation."
                lf.WriteLine(LogLine)
                Call CloseLog()
                End
            ElseIf WPPLYear > 2100 Then
                MsgBox("Invalid start year provided for WPPL.  Please rerun the model using a year between 2011 and 2100.")
                LogLine = "Invalid start year provided for WPPL.  Run terminated during intrazonal road model external variable file generation."
                lf.WriteLine(LogLine)
                Call CloseLog()
                End
            End If
        End If

        'get the input and output file names
        Call GetFiles()

        'if we are using a single scaling factor then set scaling factors - as a default they are just set to be constant over time
        If RdZPopSource = "Constant" Then
            PopGrowth = 1.005
        End If
        If RdZEcoSource = "Constant" Then
            GVAGrowth = 1.01
        End If
        If RdZEneSource = "Constant" Then
            CostGrowth = 1.01
        End If

        'if including capacity changes then read first line of the capacity file and break it down into relevant sections
        'v1.4 change this now happens automatically
        'create intermediate capacity file
        'read first line
        capstring = rzc.ReadLine
        caparray = Split(capstring, ",")
        capnum = caparray(8)
        zonecapcount = 0
        'transfer values to intermediate array
        Do Until capnum > RoadCapNum
            zonecapcount += 1
            For c = 0 To 8
                zonecapdetails(zonecapcount, c) = caparray(c)
            Next
            zonecapdetails(zonecapcount, 1) = RLCapYear(capnum)
            capstring = rzc.ReadLine
            caparray = Split(capstring, ",")
            capnum = caparray(8)
        Loop
        'then sort intermediate array by zone ID, then by year of implementation
        ReDim sortarray(zonecapcount - 1)
        For v = 0 To (zonecapcount - 1)
            padzone = String.Format("{0:000}", zonecapdetails(v, 0))
            padyear = String.Format("{0:00}", zonecapdetails(v, 1))
            sortarray(v) = padzone & "&" & padyear & "&" & v
        Next
        Array.Sort(sortarray)
        'write all lines to intermediate capacity file
        For v = 1 To (zonecapcount - 1)
            sortedline = sortarray(v)
            splitline = Split(sortedline, "&")
            arraynum = splitline(2)
            OutputRow = ""
            For c = 0 To 7
                OutputRow = OutputRow & zonecapdetails(arraynum, c) & ","
            Next
            rzcw.WriteLine(OutputRow)
        Next

        'close file
        rzcw.Close()
        rzc.Close()
        'reopen new cap file as reader
        RoadZoneNewCapData = New IO.FileStream(DirPath & EVFilePrefix & "RoadZoneCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rzcn = New IO.StreamReader(RoadZoneNewCapData, System.Text.Encoding.Default)
        'read header row
        rzcn.ReadLine()
        Call GetCapData()

        If RdZEneSource = "Database" Then
            'get base energy prices
            'v1.3 altered so that scenario file is read directly as an input file
            ZoneEneFile = New IO.FileStream(DBaseEneFile, IO.FileMode.Open, IO.FileAccess.Read)
            zer = New IO.StreamReader(ZoneEneFile, System.Text.Encoding.Default)
            'read header row
            enestring = zer.ReadLine
            'read base year prices and split into variables
            enestring = zer.ReadLine
            enearray = Split(enestring, ",")

            'get the values of the base year
            InputCount = 1
            Do While InputCount < 145
                PetOld(InputCount, 0) = enearray(1)
                DieOld(InputCount, 0) = enearray(2)
                EleOld(InputCount, 0) = enearray(3)
                LPGOld(InputCount, 0) = enearray(4)
                CNGOld(InputCount, 0) = enearray(5)
                HydOld(InputCount, 0) = enearray(6)
                InputCount += 1
            Loop

            'set base levels of fixed costs, and fuel costs will be set in the year 1 calculation of Sub CalcZoneData()
            'fixed costs
            VehFixedCosts(0, 0) = 0.7663 * 36.14
            VehFixedCosts(0, 1) = 0.7663 * 36.873
            VehFixedCosts(0, 2) = 0.7663 * 36.14
            VehFixedCosts(0, 3) = 0.7663 * 36.873
            For x = 4 To 9
                VehFixedCosts(0, x) = 0.7663 * 36.14
            Next
            For x = 0 To 9
                VehFixedCosts(1, x) = 0.845 * 61.329
            Next
            For x = 0 To 9
                VehFixedCosts(2, x) = 0.7791 * 93.665
            Next
            For x = 0 To 9
                VehFixedCosts(3, x) = 0.7065 * 109.948
            Next
            For x = 0 To 9
                VehFixedCosts(4, x) = 0.8699 * 234.5
            Next

        End If

        'Set year as 1 to start with
        Year = 1
        If WPPL = True Then
            WPPLStart = WPPLYear - 2010
        End If

        RoadInputData = New IO.FileStream(DirPath & "RoadZoneInputData2010.csv", IO.FileMode.Open, IO.FileAccess.Read)
        ri = New IO.StreamReader(RoadInputData, System.Text.Encoding.Default)
        'read header row
        InputRow = ri.ReadLine

        Do While Year < 91

            InputCount = 1

            If RdZEneSource = "Database" Then
                'read in new fuel values
                enestring = zer.ReadLine
                enearray = Split(enestring, ",")
            End If

            'read line of strategy file
            stratstring = stf.ReadLine
            stratarray = Split(stratstring, ",")

            'calculate all 144 zones
            Do Until InputCount > 144
                Call CalcZoneData()
                InputCount += 1
            Loop

            Year += 1
            ri.Close()
        Loop


        If RdZEneSource = "Database" Then
            zer.Close()
        End If
        rzcn.Close()
        ev.Close()
        stf.Close()

    End Sub

    Sub GetFiles()

        Dim outstring As String


        ExtVarOutputData = New IO.FileStream(DirPath & EVFilePrefix & "RoadZoneExtVar.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        ev = New IO.StreamWriter(ExtVarOutputData, System.Text.Encoding.Default)
        'write header row to output file
        OutputRow = "Yeary,ZoneID,PopZy,GVAZy,Costy,LaneKm,LKmMway,LKmRurAD,LKmRurAS,LKmRurMin,LKmUrbD,LKmUrbS,PCar,DCar,ECar,PLGV,DLGV,ELGV,DHGV,EHGV,DPSV,EPSV,PBike,EBike,FCBike,PHCar,DHCar," & _
            "PECar,HCar,FCCar,DHLGV,PELGV,LLGV,CLGV,DHPSV,PEPSV,LPSV,CPSV,FCPSV,DHHGV,HHGV,FCHGV"
        ev.WriteLine(OutputRow)

        RoadZoneCapData = New IO.FileStream(DirPath & CapFilePrefix & "RoadZoneCapChange.csv", IO.FileMode.Open, IO.FileAccess.Read)
        rzc = New IO.StreamReader(RoadZoneCapData, System.Text.Encoding.Default)
        'read header row
        rzc.ReadLine()

        RoadZoneNewCapData = New IO.FileStream(DirPath & EVFilePrefix & "RoadZoneCapChange.csv", IO.FileMode.CreateNew, IO.FileAccess.Write)
        rzcw = New IO.StreamWriter(RoadZoneNewCapData, System.Text.Encoding.Default)
        'write header row
        outstring = "ZoneID,ChangeYear,MWayLaneKmCh,RurADLaneKmCh,RurASLaneKmCh,RurMLaneKmCh,UrbDLaneKmCh,UrbSLaneKmCh"
        rzcw.WriteLine(outstring)

        '1.3 get the strategy file
        'open the strategy file
        StrategyFile = New IO.FileStream(DirPath & "CommonVariablesTR" & Strategy & ".csv", IO.FileMode.Open, IO.FileAccess.Read)
        stf = New IO.StreamReader(StrategyFile, System.Text.Encoding.Default)
        'read header row
        stf.ReadLine()

    End Sub
    'v1.6 to calculate by annual timesteps, parameters for each zone need to be seperated
    ' if zone number changes, the size of the array must also be changed
    ' unable to check if the code works for capyear, because the zone capacity does not changed during all 90 years

    Sub CalcZoneData()


        If Year = 1 Then
            InputRow = ri.ReadLine
            InputData = Split(InputRow, ",")
            ZoneID = InputData(0)
            PopOld(ZoneID, 0) = InputData(3)
            GVAOld(ZoneID, 0) = InputData(4)
            CostOld(ZoneID, 0) = InputData(6)
            LaneKm(ZoneID, 0) = InputData(7)
            MLaneKm(ZoneID, 0) = InputData(8)
            RurADLaneKm(ZoneID, 0) = InputData(9)
            RurASLaneKm(ZoneID, 0) = InputData(10)
            RurMinLaneKm(ZoneID, 0) = InputData(11)
            UrbDLaneKm(ZoneID, 0) = InputData(12)
            UrbSLaneKm(ZoneID, 0) = InputData(13)
            CostOld(ZoneID, 1) = InputData(18)
            CostOld(ZoneID, 2) = InputData(19)
            CostOld(ZoneID, 3) = InputData(20)
            CostOld(ZoneID, 4) = InputData(21)

            'v1.4 change set fuel efficiency old values to one
            For f = 0 To 34
                FuelEffOld(ZoneID, f) = 1
            Next

            If RdZEneSource = "Database" Then
                'calculate base fuel costs
                VehFuelCosts(InputCount, 0, 0) = 0.2337 * 36.14
                VehFuelCosts(InputCount, 0, 1) = 0.1911 * 36.873
                VehFuelCosts(InputCount, 0, 2) = ((11.2 / 18.6) * 0.2337) / (0.7663 + ((11.2 / 18.6) * 0.2337)) * 36.14
                VehFuelCosts(InputCount, 0, 3) = ((7.6 / 12.4) * 0.1911) / (0.8089 + ((7.6 / 12.4) * 0.1911)) * 36.873
                'this and later plug-in hybrids is based on petrol/diesel being used for rural roads and electricity for urban roads
                VehFuelCosts(InputCount, 0, 4) = ((((18.1 / 25.9) * (1 - InputData(17))) + ((EleOld(InputCount, 0) / PetOld(InputCount, 0)) * (46.7 / 25.9) * InputData(17))) * 0.2337) / (0.7663 + ((((18.1 / 25.9) * (1 - InputData(17))) + ((EleOld(InputCount, 0) / PetOld(InputCount, 0)) * (46.7 / 25.9) * InputData(17))) * 0.2337)) * 36.14
                PHPerOld(InputCount, 0) = ((18.1 / 25.9) * (1 - InputData(17))) / ((((18.1 / 25.9) * (1 - InputData(17))) + ((EleOld(InputCount, 0) / PetOld(InputCount, 0)) * (46.7 / 25.9) * InputData(17))))
                VehFuelCosts(InputCount, 0, 5) = ((EleOld(InputCount, 0) / PetOld(InputCount, 0)) * (16.5 / 7.3) * 0.2337) / (0.7663 + ((EleOld(InputCount, 0) / PetOld(InputCount, 0)) * (16.5 / 7.3) * 0.2337)) * 36.14
                VehFuelCosts(InputCount, 0, 8) = ((HydOld(InputCount, 0) / PetOld(InputCount, 0)) * (43.8 / 10.3) * 0.2337) / (0.7663 + ((HydOld(InputCount, 0) / PetOld(InputCount, 0)) * (43.8 / 10.3) * 0.2337)) * 36.14
                VehFuelCosts(InputCount, 0, 9) = ((HydOld(InputCount, 0) / PetOld(InputCount, 0)) * (53.3 / 25.9) * 0.2337) / (0.7663 + ((HydOld(InputCount, 0) / PetOld(InputCount, 0)) * (53.3 / 25.9) * 0.2337)) * 36.14
                VehFuelCosts(InputCount, 1, 0) = 0.155 * 61.329
                VehFuelCosts(InputCount, 1, 1) = 0.155 * 61.329
                VehFuelCosts(InputCount, 1, 3) = ((4.4 / 7.9) * 0.155) / (0.845 + ((4.4 / 7.9) * 0.155)) * 61.329
                VehFuelCosts(InputCount, 1, 4) = ((((5.8 / 7.9) * (1 - InputData(17))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (42.3 / 7.9) * InputData(17))) * 0.155) / (0.845 + ((((5.8 / 7.9) * (1 - InputData(17))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (42.3 / 7.9) * InputData(17))) * 0.155)) * 61.329
                PHPerOld(InputCount, 1) = ((5.8 / 7.9) * (1 - InputData(17))) / ((((5.8 / 7.9) * (1 - InputData(17))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (42.3 / 7.9) * InputData(17))))
                VehFuelCosts(InputCount, 1, 5) = ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (56.2 / 7.9) * 0.155) / (0.845 + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (56.2 / 7.9) * 0.155)) * 61.329
                VehFuelCosts(InputCount, 1, 6) = ((LPGOld(InputCount, 0) / DieOld(InputCount, 0)) * (11.8 / 7.9) * 0.155) / (0.845 + ((LPGOld(InputCount, 0) / DieOld(InputCount, 0)) * (11.8 / 7.9) * 0.155)) * 61.329
                VehFuelCosts(InputCount, 1, 7) = ((CNGOld(InputCount, 0) / DieOld(InputCount, 0)) * (80.8 / 7.9) * 0.155) / (0.845 + ((CNGOld(InputCount, 0) / DieOld(InputCount, 0)) * (80.8 / 7.9) * 0.155)) * 61.329
                VehFuelCosts(InputCount, 2, 1) = 0.2209 * 93.665
                VehFuelCosts(InputCount, 2, 3) = ((22.1 / 37.6) * 0.2209) / (0.7791 + ((22.1 / 37.6) * 0.2209)) * 93.665
                VehFuelCosts(InputCount, 2, 8) = ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (139.8 / 37.6) * 0.2209) / (0.7791 + ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (139.8 / 37.6) * 0.2209)) * 93.665
                VehFuelCosts(InputCount, 2, 9) = ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (112.3 / 37.6) * 0.2209) / (0.7791 + ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (112.3 / 37.6) * 0.2209)) * 93.665
                VehFuelCosts(InputCount, 3, 1) = 0.2935 * 109.948
                VehFuelCosts(InputCount, 3, 3) = ((22.1 / 37.6) * 0.2935) / (0.7065 + ((22.1 / 37.6) * 0.2935)) * 109.948
                VehFuelCosts(InputCount, 3, 8) = ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (139.8 / 37.6) * 0.2935) / (0.7065 + ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (139.8 / 37.6) * 0.2935)) * 109.948
                VehFuelCosts(InputCount, 3, 9) = ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (112.3 / 37.6) * 0.2935) / (0.7065 + ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (112.3 / 37.6) * 0.2935)) * 109.948
                VehFuelCosts(InputCount, 4, 1) = 0.1301 * 234.5
                VehFuelCosts(InputCount, 4, 3) = ((30.4 / 37.2) * 0.1301) / (0.8699 + ((30.4 / 37.2) * 0.1301)) * 234.5
                VehFuelCosts(InputCount, 4, 4) = ((((11.9 / 19.6) * (1 - InputData(17))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (103.7 / 19.6) * InputData(17))) * 0.1301) / (0.8699 + ((((11.9 / 19.6) * (1 - InputData(17))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (103.7 / 19.6) * InputData(17))) * 0.1301)) * 234.5
                PHPerOld(InputCount, 4) = ((11.9 / 19.6) * (1 - InputData(17))) / ((((11.9 / 19.6) * (1 - InputData(17))) + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (103.7 / 19.6) * InputData(17))))
                VehFuelCosts(InputCount, 4, 5) = ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (425.4 / 37.2) * 0.1301) / (0.8699 + ((EleOld(InputCount, 0) / DieOld(InputCount, 0)) * (425.4 / 37.2) * 0.1301)) * 234.5
                VehFuelCosts(InputCount, 4, 6) = ((LPGOld(InputCount, 0) / DieOld(InputCount, 0)) * (131.8 / 37.2) * 0.1301) / (0.8699 + ((LPGOld(InputCount, 0) / DieOld(InputCount, 0)) * (131.8 / 37.2) * 0.1301)) * 234.5
                VehFuelCosts(InputCount, 4, 7) = ((CNGOld(InputCount, 0) / DieOld(InputCount, 0)) * (1003.2 / 37.2) * 0.1301) / (0.8699 + ((CNGOld(InputCount, 0) / DieOld(InputCount, 0)) * (1003.2 / 37.2) * 0.1301)) * 234.5
                VehFuelCosts(InputCount, 4, 9) = ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (109.2 / 37.2) * 0.1301) / (0.8699 + ((HydOld(InputCount, 0) / DieOld(InputCount, 0)) * (109.2 / 37.2) * 0.1301)) * 234.5

            End If

        End If

        ZoneID = InputCount


        'loop through scaling up values for each year and writing to output file until the 90th year

        If RdZPopSource = "Constant" Then
            PopNew = PopOld(ZoneID, 0) * PopGrowth
        ElseIf RdZPopSource = "File" Then
            '***scaling files not currently set up for road zones module
        ElseIf RdZPopSource = "Database" Then
            'if year is after 2093 then no population forecasts are available so assume population remains constant
            'now modified as population data available up to 2100 - so should never need 'else'
            If Year < 91 Then
                keylookup = Year & "_" & ZoneID
                If PopYearLookup.TryGetValue(keylookup, newval) Then
                    PopNew = newval
                Else
                    ErrorString = "population found in lookup table for zone " & ZoneID & " in year " & Year
                    Call DictionaryMissingVal()
                End If
            Else
                PopNew = PopOld(ZoneID, 0)
            End If
        End If
        If RdZEcoSource = "Constant" Then
            GVANew = GVAOld(ZoneID, 0) * GVAGrowth
        ElseIf RdZEcoSource = "File" Then
            '***scaling files not currently set up for road zones module
        ElseIf RdZEcoSource = "Database" Then
            'if year is after 2050 then no gva forecasts are available so assume gva remains constant
            'now modified as gva data available up to 2100 - so should never need 'else'
            If Year < 91 Then
                keylookup = Year & "_" & ZoneID
                If EcoYearLookup.TryGetValue(keylookup, newval) Then
                    GVANew = newval
                Else
                    ErrorString = "gva found in lookup table for zone " & ZoneID & "in year " & Year
                    Call DictionaryMissingVal()
                End If
            Else
                GVANew = GVAOld(ZoneID, 0)
            End If
        End If

        'now amended to include different costs for different fuel types
        If RdZEneSource = "Database" Then

            PetNew = enearray(1)
            DieNew = enearray(2)
            EleNew = enearray(3)
            LPGNew = enearray(4)
            CNGNew = enearray(5)
            HydNew = enearray(6)
            'calculate ratio for each fuel
            PetRat = PetNew / PetOld(ZoneID, 0)
            DieRat = DieNew / DieOld(ZoneID, 0)
            EleRat = EleNew / EleOld(ZoneID, 0)
            LPGRat = LPGNew / LPGOld(ZoneID, 0)
            CNGRat = CNGNew / CNGOld(ZoneID, 0)
            HydRat = HydNew / HydOld(ZoneID, 0)
            'v1.4 change corrected fuel efficiency change calculation  - was previously just multiplying by figure straight from strategy array (which meant that fuel costs quickly declined to zero)
            For f = 0 To 34
                FuelEffNew(f) = stratarray(f + 31)
                FuelEffChange(f) = FuelEffNew(f) / FuelEffOld(ZoneID, f)
            Next
            'calculate cost for each vehicle type - 0 is car, 1 is LGV, 2 is small HGV, 3 is large HGV, 4 is PSV
            'calculate new cost for each fuel type within each vehicle type - 0 is petrol, 1 is diesel, 2 is petrol hybrid, 3 is diesel hybrid, 4 is plug-in hybrid, 5 is battery electric,
            '...6 is LPG, 7 is CNG, 8 is hydrogen IC, 9 is hydrogen fuel cell - by multiplying the fuel cost by the fuel ratio
            'the cost is also multiplied by changes in fuel efficiency
            VehFuelCosts(ZoneID, 0, 0) = VehFuelCosts(ZoneID, 0, 0) * PetRat * FuelEffChange(0)
            VehFuelCosts(ZoneID, 0, 1) = VehFuelCosts(ZoneID, 0, 1) * DieRat * FuelEffChange(1)
            VehFuelCosts(ZoneID, 0, 2) = VehFuelCosts(ZoneID, 0, 2) * PetRat * FuelEffChange(12)
            VehFuelCosts(ZoneID, 0, 3) = VehFuelCosts(ZoneID, 0, 3) * DieRat * FuelEffChange(13)
            PHPerNew(0) = (PHPerOld(ZoneID, 0) * VehFuelCosts(ZoneID, 0, 4) * PetRat) / ((PHPerOld(ZoneID, 0) * VehFuelCosts(ZoneID, 0, 4) * PetRat) + ((1 - PHPerOld(ZoneID, 0)) * VehFuelCosts(ZoneID, 0, 4) * EleRat))
            VehFuelCosts(ZoneID, 0, 4) = ((PHPerOld(ZoneID, 0) * VehFuelCosts(ZoneID, 0, 4) * PetRat) + ((1 - PHPerOld(ZoneID, 0)) * VehFuelCosts(ZoneID, 0, 4) * EleRat)) * FuelEffChange(14)
            VehFuelCosts(ZoneID, 0, 5) = VehFuelCosts(ZoneID, 0, 5) * EleRat * FuelEffChange(2)
            VehFuelCosts(ZoneID, 0, 8) = VehFuelCosts(ZoneID, 0, 8) * HydRat * FuelEffChange(15)
            VehFuelCosts(ZoneID, 0, 9) = VehFuelCosts(ZoneID, 0, 9) * HydRat * FuelEffChange(16)
            VehFuelCosts(ZoneID, 1, 0) = VehFuelCosts(ZoneID, 1, 0) * PetRat * FuelEffChange(3)
            VehFuelCosts(ZoneID, 1, 1) = VehFuelCosts(ZoneID, 1, 1) * DieRat * FuelEffChange(4)
            VehFuelCosts(ZoneID, 1, 3) = VehFuelCosts(ZoneID, 1, 3) * DieRat * FuelEffChange(17)
            PHPerNew(1) = (PHPerOld(ZoneID, 1) * VehFuelCosts(ZoneID, 1, 4) * DieRat) / ((PHPerOld(ZoneID, 1) * VehFuelCosts(ZoneID, 1, 4) * DieRat) + ((1 - PHPerOld(ZoneID, 1)) * VehFuelCosts(ZoneID, 1, 4) * EleRat))
            VehFuelCosts(ZoneID, 1, 4) = (PHPerOld(ZoneID, 1) * VehFuelCosts(ZoneID, 1, 4) * DieRat) + ((1 - PHPerOld(ZoneID, 1)) * VehFuelCosts(ZoneID, 1, 4) * EleRat) * FuelEffChange(18)
            VehFuelCosts(ZoneID, 1, 5) = VehFuelCosts(ZoneID, 1, 5) * EleRat * FuelEffChange(5)
            VehFuelCosts(ZoneID, 1, 6) = VehFuelCosts(ZoneID, 1, 6) * LPGRat * FuelEffChange(19)
            VehFuelCosts(ZoneID, 1, 7) = VehFuelCosts(ZoneID, 1, 7) * CNGRat * FuelEffChange(20)
            VehFuelCosts(ZoneID, 2, 1) = VehFuelCosts(ZoneID, 2, 1) * DieRat * FuelEffChange(6)
            VehFuelCosts(ZoneID, 2, 3) = VehFuelCosts(ZoneID, 2, 3) * DieRat * FuelEffChange(26)
            VehFuelCosts(ZoneID, 2, 8) = VehFuelCosts(ZoneID, 2, 8) * HydRat * FuelEffChange(27)
            VehFuelCosts(ZoneID, 2, 9) = VehFuelCosts(ZoneID, 2, 9) * HydRat * FuelEffChange(28)
            VehFuelCosts(ZoneID, 3, 1) = VehFuelCosts(ZoneID, 3, 1) * DieRat * FuelEffChange(8)
            VehFuelCosts(ZoneID, 3, 3) = VehFuelCosts(ZoneID, 3, 3) * DieRat * FuelEffChange(29)
            VehFuelCosts(ZoneID, 3, 8) = VehFuelCosts(ZoneID, 3, 8) * HydRat * FuelEffChange(31)
            VehFuelCosts(ZoneID, 3, 9) = VehFuelCosts(ZoneID, 3, 9) * HydRat * FuelEffChange(32)
            VehFuelCosts(ZoneID, 4, 1) = VehFuelCosts(ZoneID, 4, 1) * DieRat * FuelEffChange(10)
            VehFuelCosts(ZoneID, 4, 3) = VehFuelCosts(ZoneID, 4, 3) * DieRat * FuelEffChange(21)
            PHPerNew(4) = (PHPerOld(ZoneID, 4) * VehFuelCosts(ZoneID, 4, 4) * DieRat) / ((PHPerOld(ZoneID, 4) * VehFuelCosts(ZoneID, 4, 4) * DieRat) + ((1 - PHPerOld(ZoneID, 4)) * VehFuelCosts(ZoneID, 4, 4) * EleRat))
            VehFuelCosts(ZoneID, 4, 4) = (PHPerOld(ZoneID, 4) * VehFuelCosts(ZoneID, 4, 4) * DieRat) + ((1 - PHPerOld(ZoneID, 4)) * VehFuelCosts(ZoneID, 4, 4) * EleRat) * FuelEffChange(22)
            VehFuelCosts(ZoneID, 4, 5) = VehFuelCosts(ZoneID, 4, 5) * EleRat * FuelEffChange(11)
            VehFuelCosts(ZoneID, 4, 6) = VehFuelCosts(ZoneID, 4, 6) * LPGRat * FuelEffChange(23)
            VehFuelCosts(ZoneID, 4, 7) = VehFuelCosts(ZoneID, 4, 7) * CNGRat * FuelEffChange(24)
            VehFuelCosts(ZoneID, 4, 9) = VehFuelCosts(ZoneID, 4, 9) * HydRat * FuelEffChange(25)
            'v1.3 if using carbon charge then need to add that, assuming it is after the year of introduction
            If CarbonCharge = True Then
                If Year >= CarbChargeYear Then
                    'note that we assume base (2010) petrol price of 122.1 p/litre when calculating the base fuel consumption (full calculations from base figures not included in model run)
                    'calculation is: (base fuel units per km * change in fuel efficiency from base year * CO2 per unit of fuel * CO2 price per kg in pence)
                    CarbCharge(0, 0) = (0.086 * stratarray(31) * stratarray(72) * (stratarray(71) / 10))
                    CarbCharge(0, 1) = (0.057 * stratarray(32) * stratarray(73) * (stratarray(71) / 10))
                    CarbCharge(0, 2) = (0.056 * stratarray(43) * stratarray(72) * (stratarray(71) / 10))
                    CarbCharge(0, 3) = (0.038 * stratarray(44) * stratarray(73) * (stratarray(71) / 10))
                    CarbCharge(0, 4) = (PHPerOld(ZoneID, 0) * (0.06 * stratarray(45) * stratarray(72) * (stratarray(71) / 10))) + ((1 - PHPerOld(ZoneID, 0)) * (0.016 * stratarray(45) * stratarray(72) * (stratarray(70) / 10)))
                    CarbCharge(0, 5) = (0.165 * stratarray(33) * stratarray(74) * (stratarray(70) / 10))
                    CarbCharge(0, 8) = (0.438 * stratarray(46) * stratarray(77) * (stratarray(71) / 10))
                    CarbCharge(0, 9) = (0.178 * stratarray(47) * stratarray(77) * (stratarray(71) / 10))
                    CarbCharge(1, 0) = (0.088 * stratarray(34) * stratarray(72) * (stratarray(71) / 10))
                    CarbCharge(1, 1) = (0.079 * stratarray(35) * stratarray(73) * (stratarray(71) / 10))
                    CarbCharge(1, 3) = (0.044 * stratarray(48) * stratarray(73) * (stratarray(71) / 10))
                    CarbCharge(1, 4) = (PHPerOld(ZoneID, 1) * (0.058 * stratarray(49) * stratarray(73) * (stratarray(71) / 10))) + ((1 - PHPerOld(ZoneID, 1)) * (0.423 * stratarray(49) * stratarray(73) * (stratarray(70) / 10)))
                    CarbCharge(1, 5) = (0.562 * stratarray(36) * stratarray(74) * (stratarray(70) / 10))
                    CarbCharge(1, 6) = (0.118 * stratarray(50) * stratarray(75) * (stratarray(71) / 10))
                    CarbCharge(1, 7) = (0.808 * stratarray(51) * stratarray(76) * (stratarray(71) / 10))
                    CarbCharge(2, 1) = (0.259 * stratarray(37) * stratarray(73) * (stratarray(71) / 10))
                    CarbCharge(2, 3) = (0.15 * stratarray(57) * stratarray(73) * (stratarray(71) / 10))
                    CarbCharge(2, 8) = (0.957 * stratarray(58) * stratarray(77) * (stratarray(71) / 10))
                    CarbCharge(2, 9) = (0.898 * stratarray(59) * stratarray(77) * (stratarray(71) / 10))
                    CarbCharge(3, 1) = (0.376 * stratarray(39) * stratarray(73) * (stratarray(71) / 10))
                    CarbCharge(3, 3) = (0.221 * stratarray(60) * stratarray(73) * (stratarray(71) / 10))
                    CarbCharge(3, 8) = (1.398 * stratarray(61) * stratarray(77) * (stratarray(71) / 10))
                    CarbCharge(3, 9) = (1.123 * stratarray(62) * stratarray(77) * (stratarray(71) / 10))
                    CarbCharge(4, 1) = (0.176 * stratarray(41) * stratarray(73) * (stratarray(71) / 10))
                    CarbCharge(4, 3) = (0.185 * stratarray(52) * stratarray(73) * (stratarray(71) / 10))
                    CarbCharge(4, 4) = (PHPerOld(ZoneID, 4) * (0.119 * stratarray(53) * stratarray(73) * (stratarray(71) / 10))) + ((1 - PHPerOld(ZoneID, 4)) * (1.037 * stratarray(53) * stratarray(73) * (stratarray(70) / 10)))
                    CarbCharge(4, 5) = (0.2554 * stratarray(42) * stratarray(74) * (stratarray(70) / 10))
                    CarbCharge(4, 6) = (0.954 * stratarray(54) * stratarray(75) * (stratarray(71) / 10))
                    CarbCharge(4, 7) = (3.749 * stratarray(55) * stratarray(76) * (stratarray(71) / 10))
                    CarbCharge(4, 9) = (0.546 * stratarray(56) * stratarray(77) * (stratarray(71) / 10))
                End If
            End If
            'add the fixed costs
            'v1.3 and also add the carbon charge if we are using one
            If CarbonCharge = True Then
                For x = 0 To 4
                    For y = 0 To 9
                        VehCosts(x, y) = VehFixedCosts(x, y) + VehFuelCosts(ZoneID, x, y) + CarbCharge(x, y)
                    Next
                Next
            Else
                For x = 0 To 4
                    For y = 0 To 9
                        VehCosts(x, y) = VehFixedCosts(x, y) + VehFuelCosts(ZoneID, x, y)
                    Next
                Next
            End If
            'then multiply these costs by the proportions of vehicles in each fuel type (from strategy file), and aggregate the cost for each vehicle type
            CostNew(0) = (VehCosts(0, 0) * stratarray(1)) + (VehCosts(0, 1) * stratarray(2)) + (VehCosts(0, 2) * stratarray(14)) + (VehCosts(0, 3) * stratarray(15)) + (VehCosts(0, 4) * stratarray(16)) + (VehCosts(0, 5) * stratarray(3)) + (VehCosts(0, 8) * stratarray(17)) + (VehCosts(0, 9) * stratarray(18))
            CostNew(1) = (VehCosts(1, 0) * stratarray(4)) + (VehCosts(1, 1) * stratarray(5)) + (VehCosts(1, 3) * stratarray(19)) + (VehCosts(1, 4) * stratarray(20)) + (VehCosts(1, 5) * stratarray(6)) + (VehCosts(1, 6) * stratarray(21)) + (VehCosts(1, 7) * stratarray(22))
            CostNew(2) = (VehCosts(2, 1) * stratarray(7)) + (VehCosts(2, 3) * stratarray(28)) + (VehCosts(2, 8) * stratarray(29)) + (VehCosts(2, 9) * stratarray(30))
            CostNew(3) = (VehCosts(3, 1) * stratarray(7)) + (VehCosts(3, 3) * stratarray(28)) + (VehCosts(3, 8) * stratarray(29)) + (VehCosts(3, 9) * stratarray(30))
            CostNew(4) = (VehCosts(4, 1) * stratarray(9)) + (VehCosts(4, 3) * stratarray(23)) + (VehCosts(4, 4) * stratarray(24)) + (VehCosts(4, 5) * stratarray(10)) + (VehCosts(4, 6) * stratarray(25)) + (VehCosts(4, 7) * stratarray(26)) + (VehCosts(4, 9) * stratarray(27))
        Else
            For x = 0 To 4
                CostNew(x) = CostOld(ZoneID, x) * CostGrowth
            Next
        End If

        'if including capacity changes, then check if there are any capacity changes for this zone

        If ZoneID = CapID Then
            'if there are any capacity changes for this zone, check if there are any capacity changes for this year
            If Year = CapYear Then
                'if there are, then update the capacity variables, and read in the next row from the capacity file
                MLaneKm(ZoneID, 0) += MwayKmChange
                RurADLaneKm(ZoneID, 0) += RurADKmChange
                RurASLaneKm(ZoneID, 0) += RurASKmChange
                RurMinLaneKm(ZoneID, 0) += RurMinKmChange
                UrbDLaneKm(ZoneID, 0) += UrbDKmChange
                UrbSLaneKm(ZoneID, 0) += UrbSKmChange
                LaneKm(ZoneID, 0) = MLaneKm(ZoneID, 0) + RurADLaneKm(ZoneID, 0) + RurASLaneKm(ZoneID, 0) + RurMinLaneKm(ZoneID, 0) + UrbDLaneKm(ZoneID, 0) + UrbSLaneKm(ZoneID, 0)
                Call GetCapData()
            End If
        End If
        'fuel split now comes from the strategy file
        'build the fuel string from the strategy file row
        stratcount = 1
        FuelString = ""
        Do While stratcount < 31
            FuelString = FuelString & stratarray(stratcount) & ","
            stratcount += 1
        Loop
        'add in workplace parking levy if necessary
        If WPPL = True Then
            If Year >= WPPLStart Then
                UrbRoadPer = (UrbDLaneKm(ZoneID, 0) + UrbSLaneKm(ZoneID, 0)) / LaneKm(ZoneID, 0)
                'levy only applies to 20% of trips on urban roads
                WPPLTripPer = 0.2 * UrbRoadPer
                CostNew(0) = ((1 - WPPLTripPer) * CostNew(0)) + (WPPLTripPer * (CostNew(0) * (1 + (WPPLPer / 100))))
            End If
        End If
        'define fuel split - this is now specified via the strategy common variables file
        'FuelString = "0.598,0.402,0,0.055,0.945,0,1,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,"
        'write to output file
        OutputRow = ZoneID & "," & Year & "," & PopNew & "," & GVANew & "," & CostNew(0) & "," & LaneKm(ZoneID, 0) & "," & MLaneKm(ZoneID, 0) & "," & RurADLaneKm(ZoneID, 0) & "," & RurASLaneKm(ZoneID, 0) & "," & RurMinLaneKm(ZoneID, 0) & "," & UrbDLaneKm(ZoneID, 0) & "," & UrbSLaneKm(ZoneID, 0) & "," & FuelString & CostNew(1) & "," & CostNew(2) & "," & CostNew(3) & "," & CostNew(4)
        ev.WriteLine(OutputRow)
        'set old values as previous new values
        PopOld(ZoneID, 0) = PopNew
        GVAOld(ZoneID, 0) = GVANew
        PetOld(ZoneID, 0) = PetNew
        DieOld(ZoneID, 0) = DieNew
        EleOld(ZoneID, 0) = EleNew
        LPGOld(ZoneID, 0) = LPGNew
        CNGOld(ZoneID, 0) = CNGNew
        HydOld(ZoneID, 0) = HydNew
        For x = 0 To 4
            CostOld(ZoneID, x) = CostNew(x)
            PHPerOld(ZoneID, x) = PHPerNew(x)
        Next
        'v1.4 change
        For f = 0 To 34
            FuelEffOld(ZoneID, f) = FuelEffNew(f)
        Next



    End Sub

    Sub GetCapData()
        InputRow = rzcn.ReadLine
        If InputRow Is Nothing Then
        Else
            InputData = Split(InputRow, ",")
            CapID = InputData(0)
            CapYear = InputData(1)
            MwayKmChange = InputData(2)
            RurADKmChange = InputData(3)
            RurASKmChange = InputData(4)
            RurMinKmChange = InputData(5)
            UrbDKmChange = InputData(6)
            UrbSKmChange = InputData(7)
        End If
    End Sub

    Sub DictionaryMissingVal()
        LogLine = "No " & ErrorString & " when updating input files.  Model run terminated."
        lf.WriteLine(LogLine)
        lf.Close()
        MsgBox("Model run failed.  Please consult the log file for details.")
        End
    End Sub
End Module
